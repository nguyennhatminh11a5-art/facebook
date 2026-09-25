package com.minifb.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

/** Đồng hồ thật cộng thêm độ lệch điều khiển được, để test hết hạn/ân hạn mà không phải chờ. */
public class MutableClock extends Clock {

  private final AtomicReference<Duration> offset = new AtomicReference<>(Duration.ZERO);

  public void advance(Duration duration) {
    offset.updateAndGet(d -> d.plus(duration));
  }

  public void reset() {
    offset.set(Duration.ZERO);
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Instant instant() {
    return Instant.now().plus(offset.get());
  }
}
