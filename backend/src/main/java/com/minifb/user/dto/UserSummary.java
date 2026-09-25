package com.minifb.user.dto;

import com.minifb.media.MediaUrls;
import com.minifb.user.User;

/** Thông tin rút gọn của một người (tác giả bài/bình luận, danh sách bạn bè, tìm kiếm). */
public record UserSummary(long id, String displayName, String avatarUrl) {

  public static UserSummary of(User user) {
    return new UserSummary(
        user.getId(), user.getDisplayName(), MediaUrls.avatar(user.getAvatarKey()));
  }
}
