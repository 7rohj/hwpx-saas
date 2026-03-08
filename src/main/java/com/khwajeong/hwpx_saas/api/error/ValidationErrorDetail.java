package com.khwajeong.hwpx_saas.api.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ValidationErrorDetail", description = "요청 검증 오류 상세")
public class ValidationErrorDetail {
  @Schema(description = "오류가 난 필드 경로", example = "data.elements[0].placeholder")
  public String field;

  @Schema(description = "오류 메시지", example = "placeholder is required")
  public String message;

  public ValidationErrorDetail() {}
  public ValidationErrorDetail(String field, String message) {
    this.field = field;
    this.message = message;
  }
}