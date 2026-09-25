package com.minifb.common;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** MySQL DATETIME(6) chỉ giữ micro giây → luôn cắt khi gán thời gian (mục 5 quy tắc 6). */
public final class Times {

  private Times() {}

  public static Instant now(Clock clock) {
    return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
  }
}
