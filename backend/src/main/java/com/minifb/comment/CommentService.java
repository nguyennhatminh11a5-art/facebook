package com.minifb.comment;

import com.minifb.comment.dto.CommentResponse;
import com.minifb.common.BadRequestException;
import com.minifb.common.Cursor;
import com.minifb.common.ForbiddenException;
import com.minifb.common.NotFoundException;
import com.minifb.common.PageResponse;
import com.minifb.common.Times;
import com.minifb.post.Post;
import com.minifb.post.PostAccessPolicy;
import com.minifb.post.PostRepository;
import com.minifb.security.RateLimiter;
import com.minifb.security.RateLimiter.Limit;
import com.minifb.user.User;
import com.minifb.user.UserRepository;
import com.minifb.user.dto.UserSummary;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

  static final int MAX_CONTENT = 1000;

  private final CommentRepository comments;
  private final PostRepository posts;
  private final UserRepository users;
  private final PostAccessPolicy accessPolicy;
  private final RateLimiter rateLimiter;
  private final Clock clock;

  public CommentService(
      CommentRepository comments,
      PostRepository posts,
      UserRepository users,
      PostAccessPolicy accessPolicy,
      RateLimiter rateLimiter,
      Clock clock) {
    this.comments = comments;
    this.posts = posts;
    this.users = users;
    this.accessPolicy = accessPolicy;
    this.rateLimiter = rateLimiter;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResponse<CommentResponse> list(
      long viewerId, long postId, String cursor, Integer size) {
    int pageSize = Cursor.size(size);
    Post post =
        posts
            .findById(postId)
            .filter(p -> accessPolicy.canView(viewerId, p))
            .orElseThrow(CommentService::postNotFound);
    List<Comment> fetched;
    if (cursor == null) {
      fetched = comments.firstPage(postId, pageSize + 1);
    } else {
      Cursor.TimeId c = Cursor.decodeTimeId(cursor);
      fetched = comments.nextPage(postId, c.createdAt(), c.id(), pageSize + 1);
    }
    PageResponse<Comment> page =
        PageResponse.of(fetched, pageSize, c -> Cursor.encode(c.getCreatedAt(), c.getId()));
    return new PageResponse<>(toResponses(viewerId, post, page.items()), page.nextCursor());
  }

  /** Khoá bài → thêm bình luận → tăng đếm, cùng transaction (mục 5.2). */
  @Transactional
  public CommentResponse add(long viewerId, long postId, String rawContent) {
    rateLimiter.consume(Limit.CREATE_COMMENT_USER, Long.toString(viewerId));
    String content = rawContent == null ? "" : rawContent.trim();
    if (content.isEmpty() || content.length() > MAX_CONTENT) {
      throw new BadRequestException("Bình luận phải từ 1 đến " + MAX_CONTENT + " ký tự");
    }
    Post post = lockViewable(viewerId, postId);
    Comment comment = comments.save(new Comment(postId, viewerId, content, Times.now(clock)));
    posts.addCommentCount(postId, 1);
    return toResponses(viewerId, post, List.of(comment)).getFirst();
  }

  /** Người viết bình luận hoặc tác giả bài được xoá (mục 2). */
  @Transactional
  public void delete(long viewerId, long commentId) {
    Comment comment =
        comments
            .findById(commentId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy bình luận"));
    Post post = lockViewable(viewerId, comment.getPostId());
    if (!canDelete(viewerId, post, comment)) {
      throw new ForbiddenException("Bạn không có quyền xoá bình luận này");
    }
    if (comments.deleteByIdReturningCount(commentId) == 1) {
      posts.addCommentCount(post.getId(), -1);
    }
  }

  private Post lockViewable(long viewerId, long postId) {
    return posts
        .findByIdForUpdate(postId)
        .filter(p -> accessPolicy.canView(viewerId, p))
        .orElseThrow(CommentService::postNotFound);
  }

  private static boolean canDelete(long viewerId, Post post, Comment comment) {
    return comment.getUserId() == viewerId || post.getUserId() == viewerId;
  }

  private List<CommentResponse> toResponses(long viewerId, Post post, List<Comment> list) {
    if (list.isEmpty()) {
      return List.of();
    }
    Map<Long, User> authors =
        users.findByIdIn(list.stream().map(Comment::getUserId).distinct().toList()).stream()
            .collect(Collectors.toMap(User::getId, Function.identity()));
    return list.stream()
        .map(
            c ->
                new CommentResponse(
                    c.getId(),
                    c.getPostId(),
                    UserSummary.of(authors.get(c.getUserId())),
                    c.getContent(),
                    c.getCreatedAt(),
                    canDelete(viewerId, post, c)))
        .toList();
  }

  private static NotFoundException postNotFound() {
    return new NotFoundException("Không tìm thấy bài viết");
  }
}
