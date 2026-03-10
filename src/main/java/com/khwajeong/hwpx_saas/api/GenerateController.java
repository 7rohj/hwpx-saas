package com.khwajeong.hwpx_saas.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.api.dto.GenerateRequest;
import com.khwajeong.hwpx_saas.api.dto.GenerateResponse;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.api.validator.GenerateRequestValidator;
import com.khwajeong.hwpx_saas.core.ExportService;
import com.khwajeong.hwpx_saas.service.StorageService;
import com.khwajeong.hwpx_saas.service.R2StorageService;
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
    private final R2StorageService r2StorageService;

    public GenerateController(
            ExportService exportService,
            TemplateRepository templateRepo,
            StorageService storage,
            ObjectMapper om,
            R2StorageService r2StorageService
    ) {
        this.exportService = exportService;
        this.templateRepo = templateRepo;
        this.storage = storage;
        this.om = om;
        this.r2StorageService = r2StorageService;
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
            responseCode = "500",
            description = "기타 에러 (body 문법 파괴/철자 오타)",
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(
                        value = """
                        {
                          "ok": false,
                          "timestamp": "2026-03-09T15:00:00+09:00",
                          "status": 500,
                          "code": "INTERNAL_ERROR",
                          "message": "internal error",
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "문서 생성 요청 본문",
        content = @Content(
            schema = @Schema(implementation = GenerateRequest.class),
            examples = {
                @ExampleObject(
                    name = "문서 생성 요청 예시",
                    value = """
                    {
                      "templateId": "20260305_ab12cd34ef",
                      "outputFilename": "output.hwpx",
                      "elements": [
                        {
                          "placeholder": "title",
                          "type": "text",
                          "content": "문서 제목입니다",
                          "style": {
                            "font_family": "맑은 고딕",
                            "font_size": 24,
                            "bold": true,
                            "align": "center",
                            "color": "#000000"
                          }
                        },
                        {
                          "placeholder": "table3",
                          "type": "table",
                          "data": [
                            ["구분", "항목A", "항목B"],
                            ["예시1", "값1", "값2"],
                            ["예시2", "값3", "값4"]
                          ],
                          "style": {
                            "headerRow": 0,
                            "repeatHeader": true,
                            "columnWidths": [110, 110, 110],
                            "border": {
                              "outer": { "style": "hidden" },
                              "inner": { "style": "solid", "width": 0.12, "color": "#808080" },
                              "header": {
                                "bottom": { "style": "solid", "width": 0.3, "color": "#808080" },
                                "verticalInner": { "style": "solid", "width": 0.3, "color": "#FFFFFF" }
                              },
                              "lastRow": {
                                "bottom": { "style": "solid", "width": 0.3, "color": "#808080" }
                              }
                            },
                            "headerCell": {
                              "backgroundColor": "#CCCCCC",
                              "padding": { "left": 1.8, "right": 1.8, "top": 0.5, "bottom": 0.5 },
                              "textAlign": { "horizontal": "center", "vertical": "middle" }
                            },
                            "bodyCell": {
                              "backgroundColor": "#FFFFFF",
                              "padding": { "left": 1.8, "right": 1.8, "top": 0.5, "bottom": 0.5 },
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
            }
        )
    )
    public ResponseEntity<GenerateResponse> generate(@Valid @RequestBody GenerateRequest req) throws Exception {

        GenerateRequestValidator.validate(req);

        final String templateId = req.templateId.trim();

        TemplateEntity template = templateRepo.findByTemplateId(templateId)
            .orElseThrow(() -> new ApiException(
                ErrorCode.TEMPLATE_NOT_FOUND,
                "templateId 가 일치하지 않습니다.",
                "templateId 가 일치하지 않습니다. : " + templateId
            ));

        Path templatePath = storage.resolveTemplatePath(template.getStoragePath());
        storage.ensureExists(templatePath, ErrorCode.TEMPLATE_NOT_FOUND, "template file missing in storage: " + templateId);

        Set<String> vars = new LinkedHashSet<>();
        if (template.getVarsJson() != null && !template.getVarsJson().isBlank()) {
            vars = om.readValue(
                template.getVarsJson(),
                om.getTypeFactory().constructCollectionType(Set.class, String.class)
            );
        }

        List<String> missing = findMissingPlaceholders(req.elements, vars);
        if (!missing.isEmpty()) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("missingPlaceholders", missing);
            detail.put("availablePlaceholdersCount", vars.size());
            throw new ApiException(ErrorCode.INVALID_REQUEST, "템플릿에 없는 placeholder가 요청되었습니다.", detail);
        }

        String outName = (req.outputFilename == null || req.outputFilename.isBlank())
            ? "output.hwpx"
            : req.outputFilename.trim();

        if (!outName.toLowerCase().endsWith(".hwpx")) {
            outName += ".hwpx";
        }

        byte[] templateBytes = Files.readAllBytes(templatePath);

        System.out.println("=== generate request body ===");
        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(req.toBodyMap()));

        byte[] outBytes = exportService.renderHwpx(templateBytes, req.toBodyMap());

        r2StorageService.testConnection();

        String fileId = timePrefix() + "_" + safeFileName(outName);
        String objectKey = "outputs/" + fileId;

        r2StorageService.uploadHwpx(outBytes, objectKey);

        return ResponseEntity.ok(GenerateResponse.of(fileId, "/api/v1/files/" + fileId));
    }

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