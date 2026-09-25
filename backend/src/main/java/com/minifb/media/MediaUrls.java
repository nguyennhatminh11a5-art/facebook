package com.minifb.media;

/** URL tương đối (cùng domain với frontend qua reverse proxy/Vite proxy). */
public final class MediaUrls {

  public static final String AVATAR_PREFIX = "/api/v1/media/avatars/";

  private MediaUrls() {}

  /** Avatar công khai; {@code null} nếu chưa có ảnh đại diện. */
  public static String avatar(String avatarKey) {
    return avatarKey == null ? null : AVATAR_PREFIX + avatarKey;
  }
}
