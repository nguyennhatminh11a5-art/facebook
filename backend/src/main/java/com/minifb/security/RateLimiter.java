package com.minifb.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.minifb.common.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Rate limit trong bộ nhớ (Bucket4j + Caffeine). Chỉ đúng khi chạy 1 instance — xem mục 12
 * CLAUDE.md.
 */
@Component
public class RateLimiter {

  public enum Limit {
    LOGIN_FAILURE_EMAIL_IP(5, Duration.ofMinutes(15)),
    LOGIN_IP(30, Duration.ofMinutes(15)),
    REGISTER_IP(5, Duration.ofHours(1)),
    REFRESH_IP(30, Duration.ofMinutes(1)),
    CREATE_POST_USER(30, Duration.ofMinutes(1)),
    CREATE_COMMENT_USER(30, Duration.ofMinutes(1)),
    UPLOAD_USER(30, Duration.ofMinutes(1));

    private final long capacity;
    private final Duration period;

    Limit(long capacity, Duration period) {
      this.capacity = capacity;
      this.period = period;
    }
  }

  private final Map<Limit, Cache<String, Bucket>> buckets = new EnumMap<>(Limit.class);

  public RateLimiter() {
    for (Limit limit : Limit.values()) {
      buckets.put(
          limit,
          Caffeine.newBuilder()
              .expireAfterAccess(limit.period.multipliedBy(2))
              .maximumSize(100_000)
              .build());
    }
  }

  /** Trừ 1 lượt; hết lượt → 429. */
  public void consume(Limit limit, String key) {
    ConsumptionProbe probe = bucket(limit, key).tryConsumeAndReturnRemaining(1);
    if (!probe.isConsumed()) {
      throw new TooManyRequestsException(seconds(probe.getNanosToWaitForRefill()));
    }
  }

  /** Chỉ kiểm tra còn lượt, không trừ; hết lượt → 429. */
  public void ensureAvailable(Limit limit, String key) {
    EstimationProbe estimate = bucket(limit, key).estimateAbilityToConsume(1);
    if (!estimate.canBeConsumed()) {
      throw new TooManyRequestsException(seconds(estimate.getNanosToWaitForRefill()));
    }
  }

  /** Trừ 1 lượt nếu còn, không ném lỗi (đếm lần đăng nhập sai). */
  public void consumeQuietly(Limit limit, String key) {
    bucket(limit, key).tryConsume(1);
  }

  /** Dùng trong test để các test không ảnh hưởng nhau. */
  public void reset() {
    buckets.values().forEach(Cache::invalidateAll);
  }

  private Bucket bucket(Limit limit, String key) {
    return buckets
        .get(limit)
        .get(
            key,
            k ->
                Bucket.builder()
                    .addLimit(
                        Bandwidth.builder()
                            .capacity(limit.capacity)
                            .refillIntervally(limit.capacity, limit.period)
                            .build())
                    .build());
  }

  private static long seconds(long nanos) {
    return Math.max(1, Duration.ofNanos(nanos).toSeconds() + 1);
  }
}
