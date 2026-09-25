package com.minifb.auth;

import com.minifb.auth.dto.LoginRequest;
import com.minifb.auth.dto.LoginResponse;
import com.minifb.auth.dto.RegisterRequest;
import com.minifb.common.UnauthorizedException;
import com.minifb.security.CurrentUser;
import com.minifb.user.dto.MeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

  static final String REFRESH_COOKIE = "refresh_token";
  static final String COOKIE_PATH = "/api/v1/auth";

  private final AuthService authService;
  private final AuthProperties props;

  AuthController(AuthService authService, AuthProperties props) {
    this.authService = authService;
    this.props = props;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  MeResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
    return authService.register(request, http.getRemoteAddr());
  }

  @PostMapping("/login")
  ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    return toResponse(authService.login(request, http.getRemoteAddr()));
  }

  @PostMapping("/refresh")
  ResponseEntity<?> refresh(
      @CookieValue(name = REFRESH_COOKIE, required = false) String token, HttpServletRequest http) {
    try {
      return toResponse(authService.refresh(token, http.getRemoteAddr()));
    } catch (UnauthorizedException e) {
      // Xoá cookie hỏng/đã thu hồi để trình duyệt không gửi lại
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
          .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage()));
    }
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE, required = false) String token) {
    authService.logout(token, CurrentUser.id());
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
        .build();
  }

  private ResponseEntity<LoginResponse> toResponse(AuthResult result) {
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
    if (result.newRefreshToken() != null) {
      builder.header(
          HttpHeaders.SET_COOKIE, cookie(result.newRefreshToken(), props.refreshTtl()).toString());
    }
    return builder.body(new LoginResponse(result.accessToken(), result.user()));
  }

  private ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(REFRESH_COOKIE, value)
        .httpOnly(true)
        .secure(props.cookieSecure())
        .sameSite("Strict")
        .path(COOKIE_PATH)
        .maxAge(maxAge)
        .build();
  }
}
