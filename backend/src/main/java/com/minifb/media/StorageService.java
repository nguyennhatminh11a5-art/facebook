package com.minifb.media;

import java.nio.file.Path;
import java.util.Optional;

/** Lưu file theo khoá (tên file UUID). DB chỉ lưu khoá, không lưu URL tuyệt đối. */
public interface StorageService {

  void store(MediaBucket bucket, String key, byte[] data);

  /** Đường dẫn file nếu khoá hợp lệ và file tồn tại. */
  Optional<Path> find(MediaBucket bucket, String key);

  /** Xoá file; không lỗi nếu file không tồn tại. */
  void delete(MediaBucket bucket, String key);
}
