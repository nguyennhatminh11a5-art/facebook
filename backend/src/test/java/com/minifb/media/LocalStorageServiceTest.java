package com.minifb.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalStorageServiceTest {

  @TempDir Path root;

  @Test
  void storesAndFindsByBucket() {
    LocalStorageService storage =
        new LocalStorageService(new MediaProperties("x", root.toString()));
    String key = UUID.randomUUID() + ".jpg";
    storage.store(MediaBucket.POSTS, key, new byte[] {1, 2, 3});

    assertThat(storage.find(MediaBucket.POSTS, key)).isPresent();
    // Avatar bucket không đọc được ảnh bài viết
    assertThat(storage.find(MediaBucket.AVATARS, key)).isEmpty();

    storage.delete(MediaBucket.POSTS, key);
    assertThat(storage.find(MediaBucket.POSTS, key)).isEmpty();
  }

  @Test
  void rejectsTraversalAndUnknownKeys() throws Exception {
    Files.writeString(root.resolve("secret.txt"), "secret");
    LocalStorageService storage =
        new LocalStorageService(new MediaProperties("x", root.resolve("uploads").toString()));

    assertThat(storage.find(MediaBucket.POSTS, "../../secret.txt")).isEmpty();
    assertThat(storage.find(MediaBucket.POSTS, "..%2Fsecret.txt")).isEmpty();
    assertThat(storage.find(MediaBucket.POSTS, UUID.randomUUID() + ".exe")).isEmpty();
    assertThatThrownBy(() -> storage.store(MediaBucket.POSTS, "../evil.jpg", new byte[] {1}))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
