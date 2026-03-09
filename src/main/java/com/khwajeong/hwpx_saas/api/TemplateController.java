// src/main/java/com/khwajeong/hwpx_saas/api/TemplateController.java
package com.khwajeong.hwpx_saas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.service.StorageService;
import com.khwajeong.hwpx_saas.core.TemplateInspector; // 네 프로젝트에 있는 걸로 가정(없으면 TemplateInspector 대신 TemplateInspector/TemplateInspector.extractPlaceholders에 맞춰 수정)
import com.khwajeong.hwpx_saas.db.TemplateEntity;
import com.khwajeong.hwpx_saas.db.TemplateRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/template")
@Tag(name = "Template API", description = "HWPX 템플릿 관리 API")
public class TemplateController {

  private final TemplateRepository repo;
  private final StorageService storage;
  private final ObjectMapper om = new ObjectMapper();

  public TemplateController(TemplateRepository repo, StorageService storage) {
      this.repo = repo;
      this.storage = storage;
  }

  /** ✅ 업로드: templateId 발급 + 파일 저장 + vars 추출 + DB 저장 */
  @Operation(
      summary = "템플릿 업로드",
      description = "HWPX 템플릿 파일을 업로드하고 templateId를 발급합니다."
  )
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "업로드 성공",
        content = @Content(
            schema = @Schema(implementation = Map.class),
            examples = {
                @ExampleObject(
                    name = "업로드 성공 예시",
                    value = """
                    {
                      "ok": true,
                      "templateId": "20260309_ab12cd34ef",
                      "originalName": "example_template.hwpx",
                      "size": 24576,
                      "uploadedAt": "2026-03-09T18:10:00+09:00",
                      "varsCount": 4,
                      "vars": [
                        "title",
                        "table_example",
                        "author",
                        "date"
                      ]
                    }
                    """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류 / 확장자 오류 등",
        content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = {
                @ExampleObject(
                    name = "서버 오류 예시",
                    value = """
                    {
                      "ok": false,
                      "timestamp": "2026-03-09T18:14:00+09:00",
                      "status": 500,
                      "code": "INTERNAL_SERVER_ERROR",
                      "message": "Internal server error",
                      "path": "/api/v1/template/upload"
                    }
                    """
                )
            }
        )
    )
})
  @PostMapping(
      value = "/upload",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Map<String, Object>> upload(
      @Parameter(description = "업로드할 HWPX 템플릿 파일", required = true)
      @RequestPart("template") MultipartFile template,

      @Parameter(description = "placeholder 시작 토큰", example = "{{")
      @RequestParam(defaultValue = "{{") String open,

      @Parameter(description = "placeholder 종료 토큰", example = "}}")
      @RequestParam(defaultValue = "}}") String close
  ) throws Exception {

      if (template == null || template.isEmpty()) {
          throw new ApiException(ErrorCode.BAD_REQUEST, "template is empty");
      }

      String original = template.getOriginalFilename();

      if (original == null || !original.toLowerCase().endsWith(".hwpx")) {
          throw new ApiException(ErrorCode.BAD_REQUEST, "only .hwpx file is allowed");
      }
      
      String safeOriginal = safeFileName(original == null ? "template.hwpx" : original);

      String templateId = timePrefix() + "_" +
              UUID.randomUUID().toString().replace("-", "").substring(0, 10);

      String storedFileName = templateId + "_" + safeOriginal;
      Path saved = storage.templatesDir().resolve(storedFileName);

      template.transferTo(saved);

      // ✅ vars 추출 (업로드 시 1회만)
      Set<String> vars = TemplateInspector.extractPlaceholders(saved, open, close);
      String varsJson = om.writeValueAsString(vars);

      TemplateEntity entity = new TemplateEntity(
          templateId,
          original == null ? "template.hwpx" : original,
          saved.toString(),
          Files.size(saved),
          OffsetDateTime.now(),
          varsJson
      );

      repo.save(entity);

      Map<String, Object> res = new LinkedHashMap<>();
      res.put("ok", true);
      res.put("templateId", templateId);
      res.put("originalName", entity.getOriginalName());
      res.put("size", entity.getSizeBytes());
      res.put("uploadedAt", entity.getUploadedAt().toString());
      res.put("varsCount", vars.size());
      res.put("vars", vars);

      return ResponseEntity.ok(res);
  }

  /** ✅ 목록 */
  @Operation(
      summary = "템플릿 목록 조회",
      description = "업로드된 템플릿 목록을 조회합니다."
  )
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            schema = @Schema(implementation = Map.class),
            examples = {
                @ExampleObject(
                    name = "조회 성공 예시",
                    value = """
                    {
                      "ok": true,
                      "count": 2,
                      "items": [
                        {
                          "templateId": "20260309_ab12cd34ef",
                          "originalName": "example_template.hwpx",
                          "size": 24576,
                          "uploadedAt": "2026-03-09T18:10:00+09:00"
                        },
                        {
                          "templateId": "20260308_fa98bd12ac",
                          "originalName": "report_template.hwpx",
                          "size": 18200,
                          "uploadedAt": "2026-03-08T15:22:10+09:00"
                        }
                      ]
                    }
                    """
                ),
                @ExampleObject(
                    name = "템플릿 없음 예시",
                    value = """
                    {
                      "ok": true,
                      "count": 0,
                      "items": []
                    }
                    """
                )
            }
        )
    )
})
  @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> list() {
    List<TemplateEntity> all = repo.findAll();
    all.sort((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()));

    List<Map<String, Object>> items = new ArrayList<>();
    for (TemplateEntity e : all) {
      Map<String, Object> it = new LinkedHashMap<>();
      it.put("templateId", e.getTemplateId());
      it.put("originalName", e.getOriginalName());
      it.put("size", e.getSizeBytes());
      it.put("uploadedAt", e.getUploadedAt().toString());
      items.add(it);
    }

    Map<String, Object> res = new LinkedHashMap<>();
    res.put("ok", true);
    res.put("count", items.size());
    res.put("items", items);
    return ResponseEntity.ok(res);
  }

  /** ✅ 상세 조회 */
  @Operation(
      summary = "템플릿 개별 조회",
      description = "업로드된 개별 템플릿을 조회합니다."
  )
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            schema = @Schema(implementation = Map.class),
            examples = {
                @ExampleObject(
                    name = "조회 성공 예시",
                    value = """
                    {
                      "ok": true,
                      "templateId": "20260309_ab12cd34ef",
                      "originalName": "example_template.hwpx",
                      "size": 24576,
                      "uploadedAt": "2026-03-09T18:10:00+09:00",
                      "varsCount": 4,
                      "vars": [
                        "title",
                        "table_example",
                        "author",
                        "date"
                      ]
                    }
                    """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "존재하지 않는 templateId",
        content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = {
                @ExampleObject(
                    name = "템플릿 없음 예시",
                    value = """
                    {
                      "ok": false,
                      "timestamp": "2026-03-09T18:30:00+09:00",
                      "status": 404,
                      "code": "TEMPLATE_NOT_FOUND",
                      "message": "template not found: 20260309_ab12cd34ef",
                      "path": "/api/v1/template/20260309_ab12cd34ef"
                    }
                    """
                )
            }
        )
    )
})
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> detail(@PathVariable("id") String templateId) throws Exception {
    TemplateEntity e = repo.findByTemplateId(templateId)
        .orElseThrow(() -> new ApiException(ErrorCode.TEMPLATE_NOT_FOUND, "template not found: " + templateId));

    // 파일도 실제로 존재하는지 확인
    Path p = storage.resolveTemplatePath(e.getStoragePath());
    storage.ensureExists(p, ErrorCode.TEMPLATE_NOT_FOUND, "template file missing in storage: " + templateId);

    Set<String> vars = new LinkedHashSet<>();
    if (e.getVarsJson() != null && !e.getVarsJson().isBlank()) {
      vars = om.readValue(e.getVarsJson(), om.getTypeFactory().constructCollectionType(Set.class, String.class));
    }

    Map<String, Object> res = new LinkedHashMap<>();
    res.put("ok", true);
    res.put("templateId", e.getTemplateId());
    res.put("originalName", e.getOriginalName());
    res.put("size", e.getSizeBytes());
    res.put("uploadedAt", e.getUploadedAt().toString());
    res.put("varsCount", vars.size());
    res.put("vars", vars);
    return ResponseEntity.ok(res);
  }

  /** ✅ 삭제: DB + storage 파일 같이 삭제 */
  @Operation(
    summary = "템플릿 삭제",
    description = "templateId에 해당하는 템플릿을 DB와 storage에서 모두 삭제합니다."
  )
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "삭제 성공",
        content = @Content(
            schema = @Schema(implementation = Map.class),
            examples = {
                @ExampleObject(
                    name = "삭제 성공 예시",
                    value = """
                    {
                      "ok": true,
                      "deletedTemplateId": "20260309_ab12cd34ef"
                    }
                    """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "존재하지 않는 templateId",
        content = @Content(
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = {
                @ExampleObject(
                    name = "템플릿 없음 예시",
                    value = """
                    {
                      "ok": false,
                      "timestamp": "2026-03-09T18:40:00+09:00",
                      "status": 404,
                      "code": "TEMPLATE_NOT_FOUND",
                      "message": "template not found: 20260309_ab12cd34ef",
                      "path": "/api/v1/template/20260309_ab12cd34ef"
                    }
                    """
                )
            }
        )
    )
})
  @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") String templateId) throws Exception {
    TemplateEntity e = repo.findByTemplateId(templateId)
        .orElseThrow(() -> new ApiException(ErrorCode.TEMPLATE_NOT_FOUND, "template not found: " + templateId));

    Path p = storage.resolveTemplatePath(e.getStoragePath());

    // 1) DB 삭제
    repo.deleteByTemplateId(templateId);

    // 2) 파일 삭제 (없어도 OK)
    try { Files.deleteIfExists(p); } catch (Exception ignored) {}

    Map<String, Object> res = new LinkedHashMap<>();
    res.put("ok", true);
    res.put("deletedTemplateId", templateId);
    return ResponseEntity.ok(res);
  }
  
  // (선택) 디버그용으로 남겨도 됨: 업로드 안 하고 vars만 확인하고 싶을 때
  @io.swagger.v3.oas.annotations.Hidden                     //  ui 숨기기
  @org.springframework.context.annotation.Profile("dev")    // 개발환경에서만 열기
  @PostMapping(
      value = "/vars",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Map<String, Object>> vars(
      @RequestPart("template") MultipartFile template,
      @RequestParam(defaultValue = "{{") String open,
      @RequestParam(defaultValue = "}}") String close
  ) throws Exception {

    if (template == null || template.isEmpty()) {
      throw new ApiException(ErrorCode.BAD_REQUEST, "template is empty");
    }

    Path tmp = Files.createTempFile("template-", ".hwpx");
    try {
      template.transferTo(tmp);
      Set<String> vars = TemplateInspector.extractPlaceholders(tmp, open, close);

      Map<String, Object> res = new LinkedHashMap<>();
      res.put("ok", true);
      res.put("count", vars.size());
      res.put("vars", vars);
      return ResponseEntity.ok(res);
    } finally {
      try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
    }
  }

  @Operation(
    summary = "템플릿 변수 조회",
    description = "templateId에 해당하는 템플릿에서 추출된 placeholder(vars) 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "placeholder 목록 조회 성공",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "성공 예시",
                        value = """
                        {
                        "templateId": "20260305_ab12cd34ef",
                        "placeholders": [
                            "title",
                            "table_example",
                            "author",
                            "date"
                        ]
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 templateId",
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "templateId 없음 예시",
                        value = """
                        {
                        "ok": false,
                        "timestamp": "2026-03-09T15:00:00+09:00",
                        "status": 404,
                        "code": "TEMPLATE_NOT_FOUND",
                        "message": "template not found: 20260305_ab12cd34ef",
                        "path": "/api/v1/template/20260305_ab12cd34ef/vars"
                        }
                        """
                    )
                }
            )
        )
    })

@GetMapping(value = "/{id}/vars", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> varsByTemplateId(
      @Parameter(description = "템플릿 ID", required = true)
      @PathVariable("id") String templateId
  ) throws Exception {

      TemplateEntity e = repo.findByTemplateId(templateId)
          .orElseThrow(() -> new ApiException(
              ErrorCode.TEMPLATE_NOT_FOUND,
              "template not found: " + templateId
          ));

      Set<String> vars = new LinkedHashSet<>();
      if (e.getVarsJson() != null && !e.getVarsJson().isBlank()) {
          vars = om.readValue(
              e.getVarsJson(),
              om.getTypeFactory().constructCollectionType(Set.class, String.class)
          );
      }

      Map<String, Object> res = new LinkedHashMap<>();
      res.put("ok", true);
      res.put("templateId", e.getTemplateId());
      res.put("originalName", e.getOriginalName());
      res.put("varsCount", vars.size());
      res.put("vars", vars);

      return ResponseEntity.ok(res);
  }

  private static String timePrefix() {
    return java.time.LocalDateTime.now()
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
  }

  private static String safeFileName(String name) {
    String n = name.trim();
    if (n.isBlank()) return "template.hwpx";
    n = n.replaceAll("[\\\\/:*?\"<>|]", "_");
    if (!n.toLowerCase().endsWith(".hwpx")) n += ".hwpx";
    return n;
  }
}