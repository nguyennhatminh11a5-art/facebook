package com.minifb.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param refreshTtl thời hạn refresh token (7 ngày)
 * @param reuseGrace ân hạn cho token ROTATED bị dùng lại (10 giây, mục 6.1)
 * @param cookieSecure thuộc tính Secure của cookie refresh
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(Duration refreshTtl, Duration reuseGrace, boolean cookieSecure) {}
