package com.minifb.media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Lưu file trong thư mục {@code uploads/}, chống path traversal. */
@Service
public class LocalStorageService implements StorageService {

  /** Khoá do server sinh: UUID + hậu tố thumbnail tuỳ chọn + đuôi định dạng đã mã hoá lại. */
  static final Pattern KEY_PATTERN =
      Pattern.compile(
          "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(_t)?\\.(jpg|png)$");

  private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

  private final Path root;

  public LocalStorageService(MediaProperties props) {
    this.root = Path.of(props.root()).toAbsolutePath().normalize();
  }

  public static boolean isValidKey(String key) {
    return key != null && KEY_PATTERN.matcher(key).matches();
  }

  @Override
  public void store(MediaBucket bucket, String key, byte[] data) {
    Path target = resolve(bucket, key).orElseThrow(() -> new IllegalArgumentException("bad key"));
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    } catch (IOException e) {
      throw new UncheckedIOException("Không lưu được file", e);
    }
  }

  @Override
  public Optional<Path> find(MediaBucket bucket, String key) {
    return resolve(bucket, key).filter(Files::isRegularFile);
  }

  @Override
  public void delete(MediaBucket bucket, String key) {
    resolve(bucket, key)
        .ifPresent(
            path -> {
              try {
                Files.deleteIfExists(path);
              } catch (IOException e) {
                log.warn("Could not delete media file {}/{}", bucket.directory(), key, e);
              }
            });
  }

  /** Khoá phải khớp mẫu và đường dẫn chuẩn hoá phải nằm trong thư mục của bucket. */
  private Optional<Path> resolve(MediaBucket bucket, String key) {
    if (!isValidKey(key)) {
      return Optional.empty();
    }
    Path dir = root.resolve(bucket.directory()).normalize();
    Path path = dir.resolve(key).normalize();
    if (!path.startsWith(dir)) {
      return Optional.empty();
    }
    return Optional.of(path);
  }
}
