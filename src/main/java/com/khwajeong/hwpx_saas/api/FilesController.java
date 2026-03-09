package com.khwajeong.hwpx_saas.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

import com.khwajeong.hwpx_saas.api.error.ApiErrorResponse;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files API", description = "생성된 파일 다운로드 API")
public class FilesController {

  private final StorageService storage;

  public FilesController(StorageService storage) {
    this.storage = storage;
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
                      "message": "file not found: 20260309_171249_107_출력문서.hwp",
                      "path": "/api/v1/files/20260309_171249_107_%EC%B6%9C%EB%A0%A5%EB%AC%B8%EC%84%9C.hwp"
                    }
                    """
                )
            }
        )
    )
})
  @GetMapping("/{fileId}")
  public ResponseEntity<Resource> download(@PathVariable String fileId) throws Exception {
    Path p = storage.resolveFilePath(fileId);
    storage.ensureExists(p, ErrorCode.FILE_NOT_FOUND, "file not found: " + fileId);

    Resource res = new UrlResource(p.toUri());
    if (!res.exists()) throw new ApiException(ErrorCode.FILE_NOT_FOUND, "file not found: " + fileId);

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + "\"")
        .body(res);
  }
}