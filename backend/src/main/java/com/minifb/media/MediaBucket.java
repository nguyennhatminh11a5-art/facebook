package com.minifb.media;

/**
 * Nhóm file tách thư mục: endpoint avatar công khai chỉ đọc được {@link #AVATARS}, không bao giờ
 * đọc được ảnh bài viết (phải qua URL ký).
 */
public enum MediaBucket {
  POSTS("posts"),
  AVATARS("avatars");

  private final String directory;

  MediaBucket(String directory) {
    this.directory = directory;
  }

  public String directory() {
    return directory;
  }
}
