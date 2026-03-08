package com.khwajeong.hwpx_saas.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GenerateResponse", description = "문서 생성 성공 응답")
public class GenerateResponse {

  @Schema(example = "true")
  public boolean ok = true;

  @Schema(description = "서버에 저장된 파일 ID", example = "20260305_145612_123_출력문서.hwpx")
  public String fileId;

  @Schema(description = "다운로드 URL", example = "/api/v1/files/20260305_145612_123_출력문서.hwpx")
  public String downloadUrl;

  public static GenerateResponse of(String fileId, String downloadUrl) {
    GenerateResponse r = new GenerateResponse();
    r.fileId = fileId;
    r.downloadUrl = downloadUrl;
    return r;
  }
}