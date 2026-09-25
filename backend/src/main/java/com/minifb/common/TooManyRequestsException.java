package com.minifb.common;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {

  private static final long serialVersionUID = 1L;

  private final long retryAfterSeconds;

  public TooManyRequestsException(long retryAfterSeconds) {
    super(HttpStatus.TOO_MANY_REQUESTS, "Bạn thao tác quá nhanh, vui lòng thử lại sau");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
