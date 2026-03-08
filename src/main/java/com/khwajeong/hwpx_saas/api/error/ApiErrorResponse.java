package com.khwajeong.hwpx_saas.api.error;

import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)

@Schema(name = "ApiErrorResponse", description = "표준 에러 응답")
public class ApiErrorResponse {

  @Schema(description = "요청 성공 여부", example = "false")
  public boolean ok;

  @Schema(description = "에러 발생 시간", example = "2026-03-05T14:50:21.123+09:00")
  public String timestamp;

  @Schema(description = "HTTP 상태 코드", example = "400")
  public int status;

  @Schema(description = "에러 코드", example = "BAD_REQUEST")
  public String code;

  @Schema(description = "에러 메시지", example = "templateId is required")
  public String message;

  @Schema(description = "추가 상세 정보", example = "template not found")
  public Object detail;

  @Schema(description = "요청 경로", example = "/api/v1/generate")
  public String path;

  public static ApiErrorResponse of(ErrorCode code, String message, Object detail, String path) {
    ApiErrorResponse r = new ApiErrorResponse();
    r.ok = false;
    r.timestamp = OffsetDateTime.now().toString();
    r.status = code.status().value();
    r.code = code.name();
    r.message = message;
    r.detail = detail;
    r.path = path;
    return r;
  }
}