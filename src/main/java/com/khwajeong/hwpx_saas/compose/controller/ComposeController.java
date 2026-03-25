package com.khwajeong.hwpx_saas.compose.controller;

import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyResponse;
import com.khwajeong.hwpx_saas.compose.service.ComposeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;

import com.khwajeong.hwpx_saas.compose.dto.ComposePreviewResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionsResponse;
import com.khwajeong.hwpx_saas.compose.service.ComposeSelectionService;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1/compose")
public class ComposeController {

    private final ComposeService composeService;
    private final ComposeSelectionService composeSelectionService;

    public ComposeController(
            ComposeService composeService,
            ComposeSelectionService composeSelectionService
    ) {
        this.composeService = composeService;
        this.composeSelectionService = composeSelectionService;
    }

    @PostMapping("/heading1/apply")
    public ResponseEntity<ComposeApplyResponse> applyHeading1Style(
            @RequestBody ComposeApplyRequest request
    ) {
        ComposeApplyResponse response = composeService.applyHeading1Style(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(
        value = "/extract",
        consumes = "multipart/form-data"
    )
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file) throws Exception {
        File temp = File.createTempFile("upload-", ".hwpx");
        file.transferTo(temp);

        var result = composeService.extractFromHwpx(temp);
        return ResponseEntity.ok(result);
    }

    @PostMapping(
        value = "/preview",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ComposePreviewResponse> preview(
            @RequestParam("templateFile") MultipartFile templateFile,
            @RequestParam("sourceFile") MultipartFile sourceFile
    ) {
        ComposePreviewResponse response = composeService.createPreviewSession(templateFile, sourceFile);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/selections")
    public ResponseEntity<ComposeSelectionResponse> saveSelection(
            @RequestBody ComposeSelectionRequest request
    ) {
        ComposeSelectionResponse response = composeSelectionService.save(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/selections/{sessionId}")
    public ResponseEntity<ComposeSelectionsResponse> getSelections(
            @PathVariable String sessionId
    ) {
        ComposeSelectionsResponse response = composeSelectionService.getBySessionId(sessionId);
        return ResponseEntity.ok(response);
    }
}