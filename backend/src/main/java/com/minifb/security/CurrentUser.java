package com.minifb.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** User hiện tại luôn lấy từ SecurityContext, không bao giờ từ body/query. */
public final class CurrentUser {

  private CurrentUser() {}

  public static long id() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      return Long.parseLong(jwt.getToken().getSubject());
    }
    throw new IllegalStateException("Không có người dùng đã đăng nhập");
  }
}
