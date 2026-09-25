package com.minifb.common;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends ApiException {

  private static final long serialVersionUID = 1L;

  public ForbiddenException(String message) {
    super(HttpStatus.FORBIDDEN, message);
  }
}
