package com.khwajeong.hwpx_saas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.khwajeong.hwpx_saas.core.ExportService;
import com.khwajeong.hwpx_saas.core.TemplateInspector;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Tag(name = "Export API", description = "템플릿 파일을 함께 업로드하여 즉시 HWPX를 생성하는 API (레거시/테스트용)")
@RestController
@RequestMapping("/api/v1")
public class ExportController {

  private final ExportService exportService;
  private final ObjectMapper objectMapper;

  public ExportController(ExportService exportService, ObjectMapper objectMapper) {
    this.exportService = exportService;
    this.objectMapper = objectMapper;
  }

  @Operation(
      summary = "문서 생성 (템플릿 파일 포함)",
      description = "form-data로 template(file) + data(JSON 문자열)를 받아 HWPX를 즉시 생성합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "생성 성공 (HWPX 바이너리 반환)"),
      @ApiResponse(
          responseCode = "400",
          description = "입력 데이터 오류(JSON 파싱 실패 등)",
          content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
      ),
      @ApiResponse(
          responseCode = "500",
          description = "서버 오류",
          content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
      )
  })
  @PostMapping(
      value = "/export",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
  )
  public ResponseEntity<byte[]> export(
      @Parameter(description = "HWPX 템플릿 파일", required = true)
      @RequestPart("template") MultipartFile template,

      @Parameter(
          description = "JSON 문자열. 예: {\"output_filename\":\"out.hwpx\",\"elements\":[...],\"global_variables\":{...}}",
          required = true
      )
      @RequestPart("data") String dataJson
  ) {
    try {
      Map<String, Object> data =
          objectMapper.readValue(dataJson, new TypeReference<Map<String, Object>>() {});

      byte[] out = exportService.renderHwpx(template, data);

      String outName = safeFileName(str(data.get("output_filename")));
      if (outName.isBlank()) outName = "output.hwpx";

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outName + "\"")
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(out);

    } catch (Exception e) {
      // 레거시 API는 바이너리 응답이 기본이라, 에러도 최소한으로 JSON 형태 바이트로 내려줌
      String msg = e.getMessage() == null ? "internal error" : e.getMessage().replace("\"", "'");
      return ResponseEntity.status(500)
          .contentType(MediaType.APPLICATION_JSON)
          .body(("{\"ok\":false,\"message\":\"" + msg + "\"}").getBytes());
    }
  }

  /*
  // -----------------------------
  // 기존: 템플릿 vars 추출 (중복 기능)
  // POST /api/v1/template/vars
  // -----------------------------
 
  @PostMapping(value = "/vars", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> extractVars(
      @RequestPart("template") MultipartFile template
  ) throws Exception {

    Path tmp = Files.createTempFile("template-", ".hwpx");
    try {
      template.transferTo(tmp);

      Set<String> vars = TemplateInspector.extractPlaceholders(tmp, "{{", "}}");

      Map<String, Object> resp = new HashMap<>();
      resp.put("vars", vars);
      resp.put("count", vars.size());
      return ResponseEntity.ok(resp);

    } finally {
      try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
    }
  }
  */

  // 추가: templateId + data.json 으로 생성
  // POST /api/v1/generate
  // body(raw JSON):
  // {
  //   "templateId": "20260303_..._template.hwpx",
  //   "output_filename": "출력문서.hwpx",
  //   "elements": [...],
  //   "global_variables": {...}
  // }
  /*
  @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> generate(@RequestBody Map<String, Object> req) {
    try {
      String templateId = str(req.get("templateId"));
      if (templateId.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "templateId is required"));
      }

      Path templatePath = TEMPLATES_DIR.resolve(templateId);
      if (!Files.exists(templatePath)) {
        return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "template not found: " + templateId));
      }

      // output filename
      String outName = str(req.get("output_filename"));
      if (outName.isBlank()) outName = "output.hwpx";
      outName = safeFileName(outName);

      // 핵심: ExportService는 request 안에 elements/global_variables를 기대
      byte[] templateBytes = Files.readAllBytes(templatePath);
      byte[] outBytes = exportService.renderHwpx(templateBytes, req);

      String fileId = timePrefix() + "_" + outName;
      Path saved = OUTPUTS_DIR.resolve(fileId);
      Files.write(saved, outBytes);

      Map<String, Object> res = new LinkedHashMap<>();
      res.put("ok", true);
      res.put("fileId", fileId);
      res.put("downloadUrl", "/api/v1/files/" + fileId);
      return ResponseEntity.ok(res);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(500).body(Map.of("ok", false, "message", e.getMessage()));
    }
  }
  */

  /*
  // 추가: 생성된 파일 다운로드 -> FilesController로 상태 이전
  // GET /api/v1/files/{fileId}
  @GetMapping("/files/{fileId}")
  public ResponseEntity<Resource> download(@PathVariable String fileId) {
    try {
      Path path = OUTPUTS_DIR.resolve(fileId);
      if (!Files.exists(path)) return ResponseEntity.notFound().build();

      Resource res = new FileSystemResource(path.toFile());
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + "\"")
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(res);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(500).build();
    }
  }
  */

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