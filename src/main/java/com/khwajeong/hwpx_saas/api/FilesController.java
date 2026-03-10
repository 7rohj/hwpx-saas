package com.khwajeong.hwpx_saas.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.service.R2StorageService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files API", description = "생성된 파일 다운로드 API")
public class FilesController {

    private final R2StorageService r2StorageService;

    public FilesController(R2StorageService r2StorageService) {
        this.r2StorageService = r2StorageService;
    }

    @Operation(
        summary = "파일 다운로드",
        description = "generate API로 생성된 파일을 다운로드합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "다운로드 성공",
            content = @Content(
                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "파일 없음",
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponse.class),
                examples = {
                    @ExampleObject(
                        value = """
                        {
                          "ok": false,
                          "timestamp": "2026-03-09T17:13:12.563902200+09:00",
                          "status": 404,
                          "code": "FILE_NOT_FOUND",
                          "message": "file not found: 20260309_171249_107_출력문서.hwpx",
                          "path": "/api/v1/files/20260309_171249_107_%EC%B6%9C%EB%A0%A5%EB%AC%B8%EC%84%9C.hwpx"
                        }
                        """
                    )
                }
            )
        )
    })
    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> download(@PathVariable String fileId) throws Exception {
        String objectKey = "outputs/" + fileId;

        if (!r2StorageService.exists(objectKey)) {
            throw new ApiException(ErrorCode.FILE_NOT_FOUND, "file not found: " + fileId);
        }

        byte[] fileBytes = r2StorageService.downloadFile(objectKey);

        String actualFileName = fileId;
        String encodedFileName = URLEncoder.encode(actualFileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(fileBytes.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + actualFileName + "\"; filename*=UTF-8''" + encodedFileName
                )
                .body(fileBytes);
    }
}