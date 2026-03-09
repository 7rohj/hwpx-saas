package com.khwajeong.hwpx_saas.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Schema(name = "GenerateRequest", description = "템플릿 기반 문서 생성 요청")
public class GenerateRequest {

  public List<Map<String, Object>> elements;
  public Map<String, Object> global_variables;
  public String output_filename;

  @Schema(description = "템플릿 ID (Template 업로드 시 발급)", example = "20260305_ab12cd34ef")
  @NotBlank(message = "templateId 값이 필요합니다.")
  public String templateId;

  @Schema(description = "출력 파일명(.hwpx 자동 보정)", example = "출력문서.hwpx")
  public String outputFilename;

  @Schema(
      description = "템플릿에 주입할 JSON 데이터 (elements/global_variables 등)",
      //example = "{ \"elements\": [{\"placeholder\":\"title\",\"type\":\"text\",\"content\":\"문서제목\"}], \"global_variables\": {\"name\":\"홍길동\"} }"
      example = """
        {
        "templateId": "20260305_ab12cd34ef",
        "outputFilename": "example_document.hwpx",
        "elements": [
            {
            "placeholder": "title",
            "type": "text",
            "content": "title 예시입니다.",
                "style": {
                "font_family": "맑은 고딕",
                "font_size": 24,
                "bold": true,
                "align": "center",
                "color": "#000000"
                }
            },
            {
                "placeholder": "table_example",
                "type": "table",
                "data": [
                ["구분", "항목A", "항목B"],
                ["예시1", "값1", "값2"],
                ["예시2", "값3", "값4"]
                ],
                "style": {
                "headerRow": 0,
                "repeatHeader": true,
                "columnWidths": [110,110,110],
                "border": {
                    "outer": { "style": "hidden" },
                    "inner": { "style": "solid", "width": 0.12, "color": "#808080" }
                },
                "headerCell": {
                    "backgroundColor": "#CCCCCC",
                    "textAlign": { "horizontal": "center", "vertical": "middle" }
                },
                "bodyCell": {
                    "backgroundColor": "#FFFFFF",
                    "textAlign": { "horizontal": "center", "vertical": "middle" }
                }
                }
            }
        ],
        "global_variables": {
        "author": "작성자 예시",
        "date": "2026-01-01"
        }
        }
        """
  )
  //@NotNull(message = "data is required")
  //public Map<String, Object> data;

  public Map<String, Object> toBodyMap() {
    Map<String, Object> body = new LinkedHashMap<>();

    body.put("elements", elements);

    if (global_variables != null) {
        body.put("global_variables", global_variables);
    }

    if (outputFilename != null && !outputFilename.isBlank()) {
        body.put("output_filename", outputFilename);
    }

    return body;
    }
}