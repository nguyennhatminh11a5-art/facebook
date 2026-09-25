package com.minifb.user;

import com.minifb.common.BadRequestException;
import com.minifb.common.Cursor;
import com.minifb.common.NotFoundException;
import com.minifb.common.PageResponse;
import com.minifb.common.Times;
import com.minifb.friendship.FriendStatus;
import com.minifb.friendship.FriendshipRepository;
import com.minifb.media.ImageProcessor;
import com.minifb.media.MediaBucket;
import com.minifb.media.MediaFiles;
import com.minifb.media.ProcessedImage;
import com.minifb.security.RateLimiter;
import com.minifb.security.RateLimiter.Limit;
import com.minifb.user.dto.MeResponse;
import com.minifb.user.dto.ProfileResponse;
import com.minifb.user.dto.UpdateProfileRequest;
import com.minifb.user.dto.UserSummary;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

  static final int MIN_QUERY = 2;
  static final int MAX_QUERY = 50;

  private final UserRepository users;
  private final FriendshipRepository friendships;
  private final ImageProcessor imageProcessor;
  private final MediaFiles mediaFiles;
  private final RateLimiter rateLimiter;
  private final Clock clock;

  public UserService(
      UserRepository users,
      FriendshipRepository friendships,
      ImageProcessor imageProcessor,
      MediaFiles mediaFiles,
      RateLimiter rateLimiter,
      Clock clock) {
    this.users = users;
    this.friendships = friendships;
    this.imageProcessor = imageProcessor;
    this.mediaFiles = mediaFiles;
    this.rateLimiter = rateLimiter;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public MeResponse me(long currentUserId) {
    return MeResponse.of(require(currentUserId));
  }

  @Transactional
  public MeResponse updateMe(long currentUserId, UpdateProfileRequest request) {
    User user = require(currentUserId);
    String displayName = request.displayName().trim();
    String bio = request.bio() == null ? null : request.bio().trim();
    user.updateProfile(displayName, bio == null || bio.isEmpty() ? null : bio, Times.now(clock));
    return MeResponse.of(user);
  }

  /** Avatar dùng bản 640px đã mã hoá lại; ảnh cũ bị xoá sau khi commit. */
  @Transactional
  public MeResponse changeAvatar(long currentUserId, MultipartFile file) {
    rateLimiter.consume(Limit.UPLOAD_USER, Long.toString(currentUserId));
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("Chưa chọn ảnh");
    }
    if (file.getSize() > ImageProcessor.MAX_BYTES) {
      throw new BadRequestException("Ảnh vượt quá 5 MB");
    }
    ProcessedImage image;
    try {
      image = imageProcessor.process(file.getBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    User user = require(currentUserId);
    String oldKey = user.getAvatarKey();
    String newKey =
        mediaFiles.storeSingle(MediaBucket.AVATARS, image.thumbnail(), image.extension());
    user.changeAvatar(newKey, Times.now(clock));
    users.flush();
    if (oldKey != null) {
      mediaFiles.deleteAfterCommit(MediaBucket.AVATARS, List.of(oldKey));
    }
    return MeResponse.of(user);
  }

  @Transactional(readOnly = true)
  public ProfileResponse profile(long viewerId, long userId) {
    User user = require(userId);
    FriendStatus status =
        FriendStatus.of(viewerId, userId, friendships.findBetween(viewerId, userId));
    return new ProfileResponse(
        user.getId(),
        user.getDisplayName(),
        user.getBio(),
        UserSummary.of(user).avatarUrl(),
        status);
  }

  @Transactional(readOnly = true)
  public PageResponse<UserSummary> search(String rawQuery, String cursor, Integer size) {
    int pageSize = Cursor.size(size);
    String query = rawQuery == null ? "" : rawQuery.trim();
    if (query.length() < MIN_QUERY || query.length() > MAX_QUERY) {
      throw new BadRequestException(
          "Từ khoá tìm kiếm phải từ " + MIN_QUERY + " đến " + MAX_QUERY + " ký tự");
    }
    String pattern = "%" + escapeLike(query) + "%";
    List<User> found =
        cursor == null
            ? users.searchFirstPage(pattern, pageSize + 1)
            : users.searchNextPage(pattern, Cursor.decodeId(cursor), pageSize + 1);
    return PageResponse.of(found, pageSize, u -> Cursor.encode(u.getId())).map(UserSummary::of);
  }

  /** Escape ký tự đặc biệt của LIKE: {@code \}, {@code %}, {@code _}. */
  static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  User require(long id) {
    return users.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
  }
}
