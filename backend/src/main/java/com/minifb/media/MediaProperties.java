package com.minifb.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param signingSecret base64 của ≥ 32 byte, ký URL ảnh bài viết
 * @param root thư mục lưu ảnh (mặc định {@code uploads})
 */
@ConfigurationProperties("app.media")
public record MediaProperties(String signingSecret, String root) {}
