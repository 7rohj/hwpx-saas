package com.khwajeong.hwpx_saas.api.error;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handleApi(ApiException e, HttpServletRequest req) {
    ErrorCode code = e.getCode();
    return ResponseEntity
        .status(code.status())
        .body(ApiErrorResponse.of(code, e.getMessage(), e.getDetail(), req.getRequestURI()));
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException e, HttpServletRequest req) {
    return ResponseEntity
        .badRequest()
        .body(ApiErrorResponse.of(ErrorCode.BAD_REQUEST, e.getMessage(), null, req.getRequestURI()));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest req) {
    return ResponseEntity
        .badRequest()
        .body(ApiErrorResponse.of(ErrorCode.BAD_REQUEST, e.getMessage(), null, req.getRequestURI()));
  }

  // JSON 파싱/매핑 오류 (Jackson 직접 예외 + Spring 래핑 예외 둘 다)
  @ExceptionHandler({JsonParseException.class, JsonMappingException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<ApiErrorResponse> handleJson(Exception e, HttpServletRequest req) {
    // 디테일은 너무 길면 지저분하니까 message만
    return ResponseEntity
        .badRequest()
        .body(ApiErrorResponse.of(ErrorCode.JSON_INVALID, "invalid json", e.getMessage(), req.getRequestURI()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnknown(Exception e, HttpServletRequest req) {
    return ResponseEntity
        .status(ErrorCode.INTERNAL_ERROR.status())
        .body(ApiErrorResponse.of(ErrorCode.INTERNAL_ERROR, "internal error", e.getMessage(), req.getRequestURI()));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleMediaType(
    HttpMediaTypeNotSupportedException e,
    HttpServletRequest req) {
    return ResponseEntity
        .status(ErrorCode.UNSUPPORTED_MEDIA_TYPE.status())
        .body(ApiErrorResponse.of(
            ErrorCode.UNSUPPORTED_MEDIA_TYPE,
            "Content-Type is not supported",
            e.getMessage(),
            req.getRequestURI()
        ));
  }

  @ExceptionHandler(MultipartException.class)
  public ResponseEntity<ApiErrorResponse> handleMultipart(
    MultipartException e,
    HttpServletRequest req) {
    return ResponseEntity
        .badRequest()
        .body(ApiErrorResponse.of(
            ErrorCode.BAD_REQUEST,
            "multipart parse failed",
            e.getMessage(),
            req.getRequestURI()
        ));
  }
}