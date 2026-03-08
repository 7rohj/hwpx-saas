package com.khwajeong.hwpx_saas.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handle(ApiException e, HttpServletRequest req) {
    ApiErrorResponse body = ApiErrorResponse.of(
        e.getCode(),
        e.getMessage(),
        e.getDetail(),
        req.getRequestURI()
    );
    return ResponseEntity.status(e.getCode().status()).body(body);
  }

  /** @Valid DTO 검증 실패(400) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValid(MethodArgumentNotValidException e, HttpServletRequest req) {
    Map<String, Object> detail = new LinkedHashMap<>();
    Map<String, String> fields = new LinkedHashMap<>();

    for (FieldError fe : e.getBindingResult().getFieldErrors()) {
      fields.put(fe.getField(), fe.getDefaultMessage());
    }
    detail.put("fields", fields);

    ApiErrorResponse body = ApiErrorResponse.of(
        ErrorCode.BAD_REQUEST,
        "validation failed",
        detail,
        req.getRequestURI()
    );
    return ResponseEntity.status(ErrorCode.BAD_REQUEST.status()).body(body);
  }

  /** 마지막 보루 500 */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleAll(Exception e, HttpServletRequest req) {
    ApiErrorResponse body = ApiErrorResponse.of(
        ErrorCode.INTERNAL_ERROR,
        "internal error",
        null,
        req.getRequestURI()
    );
    return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status()).body(body);
  }
}