package com.minifb.media;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Đồng bộ file với transaction: file mới lưu bị xoá nếu rollback; file cũ chỉ bị xoá sau commit.
 */
@Component
public class MediaFiles {

  /** Các file không còn được DB tham chiếu, xoá sau khi commit. */
  public record OrphanedEvent(MediaBucket bucket, List<String> keys) {
    public OrphanedEvent {
      keys = List.copyOf(keys);
    }
  }

  public record StoredImage(String key, String thumbKey, int width, int height) {}

  private final StorageService storage;
  private final ApplicationEventPublisher events;

  public MediaFiles(StorageService storage, ApplicationEventPublisher events) {
    this.storage = storage;
    this.events = events;
  }

  /** Lưu ảnh + thumbnail; phải gọi trong transaction — rollback thì file bị xoá. */
  public StoredImage store(MediaBucket bucket, ProcessedImage image) {
    String id = UUID.randomUUID().toString();
    String key = id + "." + image.extension();
    String thumbKey = id + "_t." + image.extension();
    deleteOnRollback(bucket, List.of(key, thumbKey));
    storage.store(bucket, key, image.main());
    storage.store(bucket, thumbKey, image.thumbnail());
    return new StoredImage(key, thumbKey, image.width(), image.height());
  }

  /** Lưu chỉ ảnh chính (avatar dùng ảnh 640px). */
  public String storeSingle(MediaBucket bucket, byte[] data, String extension) {
    String key = UUID.randomUUID() + "." + extension;
    deleteOnRollback(bucket, List.of(key));
    storage.store(bucket, key, data);
    return key;
  }

  /** Đánh dấu file cần xoá sau khi transaction hiện tại commit. */
  public void deleteAfterCommit(MediaBucket bucket, List<String> keys) {
    if (!keys.isEmpty()) {
      events.publishEvent(new OrphanedEvent(bucket, keys));
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  void onOrphaned(OrphanedEvent event) {
    event.keys().forEach(key -> storage.delete(event.bucket(), key));
  }

  private void deleteOnRollback(MediaBucket bucket, List<String> keys) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      throw new IllegalStateException("Lưu file phải nằm trong transaction");
    }
    List<String> copy = new ArrayList<>(keys);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) {
              copy.forEach(key -> storage.delete(bucket, key));
            }
          }
        });
  }
}
