package com.khwajeong.hwpx_saas.api.error;

public class ApiException extends RuntimeException {
  private final ErrorCode code;
  private final Object detail;

  public ApiException(ErrorCode code, String message) {
    super(message);
    this.code = code;
    this.detail = null;
  }

  public ApiException(ErrorCode code, String message, Object detail) {
    super(message);
    this.code = code;
    this.detail = detail;
  }

  public ErrorCode getCode() {
    return code;
  }

  public Object getDetail() {
    return detail;
  }
}
