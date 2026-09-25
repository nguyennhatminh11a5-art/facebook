package com.minifb.user.dto;

import com.minifb.media.MediaUrls;
import com.minifb.user.User;

/** Hồ sơ của chính mình (có email). Không bao giờ chứa passwordHash. */
public record MeResponse(long id, String email, String displayName, String bio, String avatarUrl) {

  public static MeResponse of(User user) {
    return new MeResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.getBio(),
        MediaUrls.avatar(user.getAvatarKey()));
  }
}
