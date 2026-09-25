package com.minifb.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Mọi lỗi trả về dạng ProblemDetail (RFC 7807), không lộ stack trace. Lỗi chuẩn của Spring MVC
 * (validation, sai kiểu tham số, {@code MaxUploadSizeExceededException} → 413, ...) do lớp cha xử
 * lý.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
    HttpHeaders headers = new HttpHeaders();
    if (ex instanceof TooManyRequestsException tooMany) {
      headers.set(HttpHeaders.RETRY_AFTER, Long.toString(tooMany.retryAfterSeconds()));
    }
    return ResponseEntity.status(ex.status()).headers(headers).body(body);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    ProblemDetail body =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");
    return ResponseEntity.internalServerError().body(body);
  }

  @Override
  protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Dữ liệu không hợp lệ");
    body.setProperty("errors", errors);
    return handleExceptionInternal(ex, body, headers, status, request);
  }
}
