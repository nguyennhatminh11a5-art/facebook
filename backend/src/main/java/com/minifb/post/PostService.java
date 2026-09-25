package com.minifb.post;

import com.minifb.common.BadRequestException;
import com.minifb.common.Cursor;
import com.minifb.common.ForbiddenException;
import com.minifb.common.NotFoundException;
import com.minifb.common.PageResponse;
import com.minifb.common.Times;
import com.minifb.friendship.FriendStatus;
import com.minifb.friendship.FriendshipRepository;
import com.minifb.media.ImageProcessor;
import com.minifb.media.MediaBucket;
import com.minifb.media.MediaFiles;
import com.minifb.media.MediaUrlSigner;
import com.minifb.media.ProcessedImage;
import com.minifb.post.dto.PostResponse;
import com.minifb.post.dto.UserPostsResponse;
import com.minifb.reaction.ReactionRepository;
import com.minifb.reaction.ReactionType;
import com.minifb.security.RateLimiter;
import com.minifb.security.RateLimiter.Limit;
import com.minifb.user.User;
import com.minifb.user.UserRepository;
import com.minifb.user.dto.UserSummary;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostService {

  static final int MAX_CONTENT = 5000;
  static final int MAX_IMAGES = 4;

  private final PostRepository posts;
  private final PostImageRepository images;
  private final UserRepository users;
  private final ReactionRepository reactions;
  private final FriendshipRepository friendships;
  private final PostAccessPolicy accessPolicy;
  private final ImageProcessor imageProcessor;
  private final MediaFiles mediaFiles;
  private final MediaUrlSigner signer;
  private final RateLimiter rateLimiter;
  private final Clock clock;

  public PostService(
      PostRepository posts,
      PostImageRepository images,
      UserRepository users,
      ReactionRepository reactions,
      FriendshipRepository friendships,
      PostAccessPolicy accessPolicy,
      ImageProcessor imageProcessor,
      MediaFiles mediaFiles,
      MediaUrlSigner signer,
      RateLimiter rateLimiter,
      Clock clock) {
    this.posts = posts;
    this.images = images;
    this.users = users;
    this.reactions = reactions;
    this.friendships = friendships;
    this.accessPolicy = accessPolicy;
    this.imageProcessor = imageProcessor;
    this.mediaFiles = mediaFiles;
    this.signer = signer;
    this.rateLimiter = rateLimiter;
    this.clock = clock;
  }

  /** Luồng: kiểm tra → xử lý ảnh → lưu file → ghi DB. Rollback → file vừa lưu bị xoá. */
  @Transactional
  public PostResponse create(long userId, String rawContent, List<MultipartFile> uploads) {
    rateLimiter.consume(Limit.CREATE_POST_USER, Long.toString(userId));
    String content = normalize(rawContent);
    List<MultipartFile> files =
        uploads == null ? List.of() : uploads.stream().filter(f -> !f.isEmpty()).toList();
    if (files.size() > MAX_IMAGES) {
      throw new BadRequestException("Tối đa " + MAX_IMAGES + " ảnh mỗi bài");
    }
    if (content == null && files.isEmpty()) {
      throw new BadRequestException("Bài viết cần có nội dung hoặc ít nhất 1 ảnh");
    }

    List<ProcessedImage> processed = files.stream().map(this::process).toList();

    List<MediaFiles.StoredImage> stored = new ArrayList<>();
    for (ProcessedImage image : processed) {
      stored.add(mediaFiles.store(MediaBucket.POSTS, image));
    }

    Post post = posts.save(new Post(userId, content, Times.now(clock)));
    List<PostImage> rows = new ArrayList<>();
    for (int i = 0; i < stored.size(); i++) {
      MediaFiles.StoredImage s = stored.get(i);
      rows.add(new PostImage(post.getId(), s.key(), s.thumbKey(), s.width(), s.height(), i));
    }
    images.saveAll(rows);
    posts.flush();
    return toResponses(userId, List.of(post)).getFirst();
  }

  @Transactional(readOnly = true)
  public PostResponse get(long viewerId, long postId) {
    Post post = requireViewable(viewerId, postId);
    return toResponses(viewerId, List.of(post)).getFirst();
  }

  @Transactional
  public PostResponse updateContent(long viewerId, long postId, String rawContent) {
    Post post = requireEditable(viewerId, postId);
    String content = normalize(rawContent);
    if (content == null && images.findByPostIdOrderByPositionAsc(postId).isEmpty()) {
      throw new BadRequestException("Bài không có ảnh thì không được để trống nội dung");
    }
    post.editContent(content, Times.now(clock));
    posts.flush();
    return toResponses(viewerId, List.of(post)).getFirst();
  }

  /** Xoá DB trong transaction, xoá file sau khi commit. */
  @Transactional
  public void delete(long viewerId, long postId) {
    Post post = requireEditable(viewerId, postId);
    List<String> keys = new ArrayList<>();
    for (PostImage image : images.findByPostIdOrderByPositionAsc(postId)) {
      keys.add(image.getStorageKey());
      keys.add(image.getThumbKey());
    }
    posts.delete(post);
    mediaFiles.deleteAfterCommit(MediaBucket.POSTS, keys);
  }

  /** Bảng tin: bài của tôi + bạn bè ACCEPTED. */
  @Transactional(readOnly = true)
  public PageResponse<PostResponse> feed(long viewerId, String cursor, Integer size) {
    int pageSize = Cursor.size(size);
    List<Long> authorIds = new ArrayList<>(friendships.friendIds(viewerId));
    authorIds.add(viewerId);
    List<Long> ids;
    if (cursor == null) {
      ids = posts.feedFirstPage(authorIds, pageSize + 1);
    } else {
      Cursor.TimeId c = Cursor.decodeTimeId(cursor);
      ids = posts.feedNextPage(authorIds, c.createdAt(), c.id(), pageSize + 1);
    }
    return page(viewerId, ids, pageSize);
  }

  @Transactional(readOnly = true)
  public UserPostsResponse userPosts(long viewerId, long userId, String cursor, Integer size) {
    int pageSize = Cursor.size(size);
    if (!users.existsById(userId)) {
      throw new NotFoundException("Không tìm thấy người dùng");
    }
    FriendStatus status =
        FriendStatus.of(viewerId, userId, friendships.findBetween(viewerId, userId));
    if (!accessPolicy.canViewPostsOf(viewerId, userId)) {
      return new UserPostsResponse(List.of(), null, status);
    }
    List<Long> ids;
    if (cursor == null) {
      ids = posts.userFirstPage(userId, pageSize + 1);
    } else {
      Cursor.TimeId c = Cursor.decodeTimeId(cursor);
      ids = posts.userNextPage(userId, c.createdAt(), c.id(), pageSize + 1);
    }
    PageResponse<PostResponse> page = page(viewerId, ids, pageSize);
    return new UserPostsResponse(page.items(), page.nextCursor(), status);
  }

  /** Không xem được → 404 (không lộ sự tồn tại). */
  public Post requireViewable(long viewerId, long postId) {
    Post post = posts.findById(postId).orElseThrow(PostService::notFound);
    if (!accessPolicy.canView(viewerId, post)) {
      throw notFound();
    }
    return post;
  }

  private Post requireEditable(long viewerId, long postId) {
    Post post = requireViewable(viewerId, postId);
    if (post.getUserId() != viewerId) {
      throw new ForbiddenException("Bạn không có quyền sửa/xoá bài này");
    }
    return post;
  }

  private PageResponse<PostResponse> page(long viewerId, List<Long> ids, int pageSize) {
    Map<Long, Post> byId =
        posts.findByIdIn(ids).stream().collect(Collectors.toMap(Post::getId, Function.identity()));
    List<Post> ordered = ids.stream().map(byId::get).filter(p -> p != null).toList();
    PageResponse<Post> page =
        PageResponse.of(ordered, pageSize, p -> Cursor.encode(p.getCreatedAt(), p.getId()));
    return new PageResponse<>(toResponses(viewerId, page.items()), page.nextCursor());
  }

  /** Nạp tác giả, ảnh, reaction của tôi theo lô — số truy vấn cố định, không phụ thuộc số bài. */
  private List<PostResponse> toResponses(long viewerId, List<Post> list) {
    if (list.isEmpty()) {
      return List.of();
    }
    List<Long> postIds = list.stream().map(Post::getId).toList();
    List<Long> authorIds = list.stream().map(Post::getUserId).distinct().toList();

    Map<Long, User> authors =
        users.findByIdIn(authorIds).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    Map<Long, List<PostImage>> imagesByPost =
        images.findByPostIdInOrderByPostIdAscPositionAsc(postIds).stream()
            .sorted(Comparator.comparingInt(PostImage::getPosition))
            .collect(Collectors.groupingBy(PostImage::getPostId));
    Map<Long, ReactionType> mine =
        reactions.findMine(viewerId, postIds).stream()
            .collect(
                Collectors.toMap(
                    ReactionRepository.MyReaction::getPostId,
                    r -> ReactionType.valueOf(r.getType())));

    return list.stream()
        .map(
            p ->
                new PostResponse(
                    p.getId(),
                    UserSummary.of(authors.get(p.getUserId())),
                    p.getContent(),
                    imagesByPost.getOrDefault(p.getId(), List.of()).stream()
                        .map(
                            i ->
                                new PostResponse.ImageResponse(
                                    signer.url(i.getStorageKey()),
                                    signer.url(i.getThumbKey()),
                                    i.getWidth(),
                                    i.getHeight()))
                        .toList(),
                    p.getReactionCount(),
                    p.getCommentCount(),
                    mine.get(p.getId()),
                    p.getCreatedAt(),
                    p.getUpdatedAt(),
                    p.getUserId() == viewerId))
        .toList();
  }

  private ProcessedImage process(MultipartFile file) {
    if (file.getSize() > ImageProcessor.MAX_BYTES) {
      throw new BadRequestException("Ảnh vượt quá 5 MB");
    }
    try {
      return imageProcessor.process(file.getBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** {@code trim()}; rỗng → {@code null}; > 5000 ký tự → 400. */
  private static String normalize(String raw) {
    if (raw == null) {
      return null;
    }
    String content = raw.trim();
    if (content.length() > MAX_CONTENT) {
      throw new BadRequestException("Nội dung tối đa " + MAX_CONTENT + " ký tự");
    }
    return content.isEmpty() ? null : content;
  }

  private static NotFoundException notFound() {
    return new NotFoundException("Không tìm thấy bài viết");
  }
}
