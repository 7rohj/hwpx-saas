package com.khwajeong.hwpx_saas.api.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  // 400
  BAD_REQUEST(HttpStatus.BAD_REQUEST),
  JSON_INVALID(HttpStatus.BAD_REQUEST),
  MISSING_FIELD(HttpStatus.BAD_REQUEST),

  INVALID_REQUEST(HttpStatus.BAD_REQUEST),
  INVALID_ELEMENT(HttpStatus.BAD_REQUEST),

  // 401
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED),

  // 404
  TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND),
  FILE_NOT_FOUND(HttpStatus.NOT_FOUND),

  // 413
  FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE),

  // 415
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),

  // 500
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

  private final HttpStatus status;

  ErrorCode(HttpStatus status) {
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }
}