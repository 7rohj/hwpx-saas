package com.khwajeong.hwpx_saas.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.api.dto.GenerateRequest;
import com.khwajeong.hwpx_saas.api.dto.GenerateResponse;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.api.validator.GenerateRequestValidator;
import com.khwajeong.hwpx_saas.core.ExportService;
import com.khwajeong.hwpx_saas.service.StorageService; //core.StorageService;
import com.khwajeong.hwpx_saas.db.TemplateRepository;
import com.khwajeong.hwpx_saas.db.TemplateEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Tag(name = "Generate API", description = "템플릿 기반 문서 생성 API")
@RestController
@RequestMapping("/api/v1")
public class GenerateController {

  private final ExportService exportService;
  private final TemplateRepository templateRepo;
  private final StorageService storage;
  private final ObjectMapper om;

  public GenerateController(ExportService exportService, TemplateRepository templateRepo, StorageService storage, ObjectMapper om) {
    this.exportService = exportService;
    this.templateRepo = templateRepo;
    this.storage = storage;
    this.om = om;
  }

  @Operation(
      summary = "문서 생성",
      description = "templateId + JSON 데이터를 이용해 HWPX 문서를 생성하고, fileId 및 다운로드 URL을 반환합니다."
  )
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "생성 성공",
        content = @Content(schema = @Schema(implementation = GenerateResponse.class))
    ),
    @ApiResponse(
        responseCode = "400",
        description = "요청 값 오류/검증 실패",
        content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = {
                @ExampleObject(
                    name = "검증 실패 예시",
                    value = """
                    {
                      "ok": false,
                      "timestamp": "2026-03-09T15:00:00+09:00",
                      "status": 400,
                      "code": "INVALID_REQUEST",
                      "message": "요청 검증에 실패했습니다.",
                      "detail": [
                        {
                          "field": "templateId",
                          "message": "templateId 값이 필요합니다."
                        }
                      ],
                      "path": "/api/v1/generate"
                    }
                    """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "템플릿/파일 없음",
        content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = {
                @ExampleObject(
                    name = "템플릿 없음 예시",
                    value = """
                    {
                      "ok": false,
                      "timestamp": "2026-03-09T15:00:00+09:00",
                      "status": 404,
                      "code": "TEMPLATE_NOT_FOUND",
                      "message": "템플릿을 찾을 수 없습니다.",
                      "detail": "template not found: 20260305_ab12cd34ef",
                      "path": "/api/v1/generate"
                    }
                    """
                )
            }
        )
    )
})
  @PostMapping(
      value = "/generate",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<GenerateResponse> generate(@Valid @RequestBody GenerateRequest req) throws Exception {

    GenerateRequestValidator.validate(req);

    final String templateId = req.templateId.trim();

    TemplateEntity template = templateRepo.findByTemplateId(templateId)
        .orElseThrow(() -> new ApiException(
        ErrorCode.TEMPLATE_NOT_FOUND,
        "템플릿을 찾을 수 없습니다.",
        "template not found: " + templateId
    ));

    Path templatePath = storage.resolveTemplatePath(template.getStoragePath());
    storage.ensureExists(templatePath, ErrorCode.TEMPLATE_NOT_FOUND, "template file missing in storage: " + templateId);

    // 템플릿 vars 로드
    Set<String> vars = new LinkedHashSet<>();
    if (template.getVarsJson() != null && !template.getVarsJson().isBlank()) {
      vars = om.readValue(template.getVarsJson(),
          om.getTypeFactory().constructCollectionType(Set.class, String.class));
    }

    // 요청 placeholder 검증
    List<String> missing = findMissingPlaceholders(req.elements, vars);
    if (!missing.isEmpty()) {
      Map<String, Object> detail = new LinkedHashMap<>();
      detail.put("missingPlaceholders", missing);
      detail.put("availablePlaceholdersCount", vars.size());
      //detail.put("availavlePlaceholders", vars);
      throw new ApiException(ErrorCode.INVALID_REQUEST, "템플릿에 없는 placeholder가 요청되었습니다.", detail);
    }

    // output file name
    String outName = (req.outputFilename == null || req.outputFilename.isBlank()) ? "output.hwpx" : req.outputFilename.trim();
    if (!outName.toLowerCase().endsWith(".hwpx")) outName += ".hwpx";

    byte[] templateBytes = Files.readAllBytes(templatePath);
    
    // systemout 추가 260306
    System.out.println("=== generate request body ===");
    System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(req.toBodyMap()));

    byte[] outBytes = exportService.renderHwpx(templateBytes, req.toBodyMap());

    String fileId = timePrefix() + "_" + safeFileName(outName);
    Path outPath = storage.filesDir().resolve(fileId);
    Files.write(outPath, outBytes);

    return ResponseEntity.ok(GenerateResponse.of(fileId, "/api/v1/files/" + fileId));
  }

  // ===== 유틸(기존에 있던 거 그대로 쓰면 됨) =====
  private List<String> findMissingPlaceholders(List<Map<String, Object>> elements, Set<String> templateVars) {
    if (elements == null || elements.isEmpty()) {
      return List.of();
    }

    Set<String> requested = extractRequestedPlaceholders(elements);

    List<String> missing = new ArrayList<>();
    for (String ph : requested) {
      if (!templateVars.contains(ph)) {
        missing.add(ph);
      }
    }

    Collections.sort(missing);
    return missing;
  }

  private Set<String> extractRequestedPlaceholders(List<Map<String, Object>> elements) {
    Set<String> placeholders = new LinkedHashSet<>();

    for (int i = 0; i < elements.size(); i++) {
      Map<String, Object> el = elements.get(i);
      if (el == null) continue;

      String placeholder = str(el.get("placeholder"));
      if (!placeholder.isBlank()) {
        placeholders.add(placeholder);
      }
    }

    return placeholders;
  }

  private static String str(Object v) {
    return v == null ? "" : String.valueOf(v).trim();
  }

  private static String timePrefix() {
    return java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
  }

  private static String safeFileName(String name) {
    String n = name.trim();
    if (n.isBlank()) return "output.hwpx";
    n = n.replaceAll("[\\\\/:*?\"<>|]", "_");
    if (!n.toLowerCase().endsWith(".hwpx")) n += ".hwpx";
    return n;
  }
}