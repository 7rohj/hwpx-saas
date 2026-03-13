package com.khwajeong.hwpx_saas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.core.TemplateInspector;
import com.khwajeong.hwpx_saas.db.TemplateEntity;
import com.khwajeong.hwpx_saas.db.TemplateRepository;
import com.khwajeong.hwpx_saas.db.UserEntity;
import com.khwajeong.hwpx_saas.service.ApiKeyService;
import com.khwajeong.hwpx_saas.service.StorageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/template")
@Tag(name = "Template API", description = "HWPX 템플릿 관리 API")
public class TemplateController {

    private final TemplateRepository repo;
    private final StorageService storage;
    private final ApiKeyService apiKeyService;
    private final ObjectMapper om = new ObjectMapper();

    public TemplateController(
            TemplateRepository repo,
            StorageService storage,
            ApiKeyService apiKeyService
    ) {
        this.repo = repo;
        this.storage = storage;
        this.apiKeyService = apiKeyService;
    }

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
            responseCode = "401",
            description = "사용자 API 키 없음/오류",
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponse.class)
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
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,

            @Parameter(description = "업로드할 HWPX 템플릿 파일", required = true)
            @RequestPart("template") MultipartFile template,

            @Parameter(description = "placeholder 시작 토큰", example = "{{")
            @RequestParam(defaultValue = "{{") String open,

            @Parameter(description = "placeholder 종료 토큰", example = "}}")
            @RequestParam(defaultValue = "}}") String close
    ) throws Exception {

        UserEntity user = requireUser(apiKey);

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

        Set<String> vars = TemplateInspector.extractPlaceholders(saved, open, close);
        String varsJson = om.writeValueAsString(vars);

        TemplateEntity entity = new TemplateEntity(
                user.getId(),
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

    @Operation(
        summary = "템플릿 목록 조회",
        description = "현재 사용자 본인이 업로드한 템플릿 목록을 조회합니다."
    )
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> list(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey
    ) {
        UserEntity user = requireUser(apiKey);

        List<TemplateEntity> all = repo.findByUserIdOrderByUploadedAtDesc(user.getId());

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

    @Operation(
        summary = "템플릿 개별 조회",
        description = "현재 사용자 본인의 개별 템플릿을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "사용자 API 키 없음/오류"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 templateId")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> detail(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @PathVariable("id") String templateId
    ) throws Exception {
        UserEntity user = requireUser(apiKey);

        TemplateEntity e = repo.findByTemplateIdAndUserId(templateId, user.getId())
                .orElseThrow(() -> new ApiException(
                        ErrorCode.TEMPLATE_NOT_FOUND,
                        "template not found: " + templateId
                ));

        Path p = storage.resolveTemplatePath(e.getStoragePath());
        storage.ensureExists(p, ErrorCode.TEMPLATE_NOT_FOUND, "template file missing in storage: " + templateId);

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
        res.put("size", e.getSizeBytes());
        res.put("uploadedAt", e.getUploadedAt().toString());
        res.put("varsCount", vars.size());
        res.put("vars", vars);
        return ResponseEntity.ok(res);
    }

    @Operation(
        summary = "템플릿 삭제",
        description = "현재 사용자 본인의 templateId에 해당하는 템플릿을 DB와 storage에서 모두 삭제합니다."
    )
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> delete(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @PathVariable("id") String templateId
    ) throws Exception {
        UserEntity user = requireUser(apiKey);

        TemplateEntity e = repo.findByTemplateIdAndUserId(templateId, user.getId())
                .orElseThrow(() -> new ApiException(
                        ErrorCode.TEMPLATE_NOT_FOUND,
                        "template not found: " + templateId
                ));

        Path p = storage.resolveTemplatePath(e.getStoragePath());

        repo.delete(e);

        try {
            Files.deleteIfExists(p);
        } catch (Exception ignored) {}

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("deletedTemplateId", templateId);
        return ResponseEntity.ok(res);
    }

    @io.swagger.v3.oas.annotations.Hidden
    @org.springframework.context.annotation.Profile("dev")
    @PostMapping(
        value = "/vars",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> vars(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestPart("template") MultipartFile template,
            @RequestParam(defaultValue = "{{") String open,
            @RequestParam(defaultValue = "}}") String close
    ) throws Exception {

        requireUser(apiKey);

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
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignored) {}
        }
    }

    @Operation(
        summary = "템플릿 변수 조회",
        description = "현재 사용자 본인의 templateId에 해당하는 템플릿에서 추출된 placeholder(vars) 목록을 조회합니다."
    )
    @GetMapping(value = "/{id}/vars", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> varsByTemplateId(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @Parameter(description = "템플릿 ID", required = true)
            @PathVariable("id") String templateId
    ) throws Exception {

        UserEntity user = requireUser(apiKey);

        TemplateEntity e = repo.findByTemplateIdAndUserId(templateId, user.getId())
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

    private UserEntity requireUser(String apiKey) {
        UserEntity user = apiKeyService.findUserByApiKey(apiKey);
        if (user == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "invalid or missing api key");
        }
        return user;
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