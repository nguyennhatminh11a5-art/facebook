package com.minifb.auth;

import com.minifb.user.dto.MeResponse;

/**
 * Kết quả đăng nhập/refresh.
 *
 * @param newRefreshToken token thô để đặt vào cookie; {@code null} khi không đổi cookie (dùng lại
 *     token ROTATED trong thời gian ân hạn)
 */
public record AuthResult(String accessToken, MeResponse user, String newRefreshToken) {

  @Override
  public String toString() {
    return "AuthResult[user=" + user.id() + "]";
  }
}
