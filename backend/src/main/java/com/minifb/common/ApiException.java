package com.minifb.common;

import org.springframework.http.HttpStatus;

/** Lỗi nghiệp vụ; {@link GlobalExceptionHandler} chuyển thành ProblemDetail. */
public abstract class ApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus status;

  protected ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }
}
