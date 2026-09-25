package com.minifb.security;

import java.util.Base64;

/** Bí mật HMAC phải là base64 của ≥ 32 byte ngẫu nhiên; thiếu hoặc ngắn → không khởi động. */
public final class Secrets {

  public static final int MIN_BYTES = 32;

  private Secrets() {}

  public static byte[] decode(String name, String base64) {
    if (base64 == null || base64.isBlank()) {
      throw new IllegalStateException(name + " chưa được cấu hình");
    }
    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(base64.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(name + " phải là chuỗi base64", e);
    }
    if (bytes.length < MIN_BYTES) {
      throw new IllegalStateException(name + " phải dài ít nhất " + MIN_BYTES + " byte");
    }
    return bytes;
  }
}
