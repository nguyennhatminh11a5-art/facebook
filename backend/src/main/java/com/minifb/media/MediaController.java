package com.minifb.media;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Phục vụ ảnh: ảnh bài viết qua URL ký, avatar công khai. Sai/hết hạn/không có → 404. */
@RestController
@RequestMapping("/api/v1/media")
class MediaController {

  private final StorageService storage;
  private final MediaUrlSigner signer;

  MediaController(StorageService storage, MediaUrlSigner signer) {
    this.storage = storage;
    this.signer = signer;
  }

  @GetMapping("/{key}")
  ResponseEntity<Resource> postImage(
      @PathVariable String key,
      @RequestParam(required = false) Long exp,
      @RequestParam(required = false) String sig) {
    if (exp == null || !signer.verify(key, exp, sig)) {
      return ResponseEntity.notFound().build();
    }
    return serve(MediaBucket.POSTS, key);
  }

  @GetMapping("/avatars/{key}")
  ResponseEntity<Resource> avatar(@PathVariable String key) {
    return serve(MediaBucket.AVATARS, key);
  }

  private ResponseEntity<Resource> serve(MediaBucket bucket, String key) {
    return storage
        .find(bucket, key)
        .map(
            (Path path) ->
                ResponseEntity.ok()
                    .contentType(key.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                    .header("X-Content-Type-Options", "nosniff")
                    .<Resource>body(new FileSystemResource(path)))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
