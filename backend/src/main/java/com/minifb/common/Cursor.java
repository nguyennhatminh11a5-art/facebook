package com.minifb.common;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Cursor phân trang: base64url của {@code createdAt|id} hoặc chỉ {@code id} (mục 5.1). Sai định
 * dạng → 400.
 */
public final class Cursor {

  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 50;

  private Cursor() {}

  public record TimeId(Instant createdAt, long id) {}

  public static String encode(Instant createdAt, long id) {
    return encodeRaw(createdAt.getEpochSecond() + "." + createdAt.getNano() + "|" + id);
  }

  public static String encode(long id) {
    return encodeRaw(Long.toString(id));
  }

  public static TimeId decodeTimeId(String cursor) {
    String raw = decodeRaw(cursor);
    int bar = raw.indexOf('|');
    int dot = raw.indexOf('.');
    if (bar < 0 || dot < 0 || dot > bar) {
      throw invalid();
    }
    try {
      long seconds = Long.parseLong(raw.substring(0, dot));
      int nanos = Integer.parseInt(raw.substring(dot + 1, bar));
      long id = Long.parseLong(raw.substring(bar + 1));
      return new TimeId(Instant.ofEpochSecond(seconds, nanos), id);
    } catch (RuntimeException e) {
      throw invalid();
    }
  }

  public static long decodeId(String cursor) {
    try {
      return Long.parseLong(decodeRaw(cursor));
    } catch (NumberFormatException e) {
      throw invalid();
    }
  }

  /** Chuẩn hoá {@code size}: mặc định 10, trong khoảng 1..50. */
  public static int size(Integer requested) {
    if (requested == null) {
      return DEFAULT_SIZE;
    }
    if (requested < 1 || requested > MAX_SIZE) {
      throw new BadRequestException("size phải trong khoảng 1.." + MAX_SIZE);
    }
    return requested;
  }

  private static String encodeRaw(String raw) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  private static String decodeRaw(String cursor) {
    if (cursor == null || cursor.length() > 100) {
      throw invalid();
    }
    try {
      return new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw invalid();
    }
  }

  private static BadRequestException invalid() {
    return new BadRequestException("cursor không hợp lệ");
  }
}
