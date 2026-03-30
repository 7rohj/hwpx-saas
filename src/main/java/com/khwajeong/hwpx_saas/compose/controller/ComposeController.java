package com.khwajeong.hwpx_saas.compose.controller;

import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposePreviewResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRunApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRunApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionsResponse;
import com.khwajeong.hwpx_saas.compose.service.ComposeApplyRunnerService;
import com.khwajeong.hwpx_saas.compose.service.ComposeSelectionService;
import com.khwajeong.hwpx_saas.compose.service.ComposeService;
import com.khwajeong.hwpx_saas.compose.service.ComposeSessionService;
import com.khwajeong.hwpx_saas.compose.service.HwpxTableExtractor;
import com.khwajeong.hwpx_saas.compose.service.ImageCropService;
import com.khwajeong.hwpx_saas.compose.service.OcrService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/compose")
public class ComposeController {

    private final ComposeService composeService;
    private final ComposeSelectionService composeSelectionService;
    private final ComposeApplyRunnerService composeApplyRunnerService;
    private final ImageCropService imageCropService;
    private final OcrService ocrService;
    private final ComposeSessionService composeSessionService;

    // 추가:
    // 표 추출 서비스 주입
    private final HwpxTableExtractor hwpxTableExtractor;

    public ComposeController(
            ComposeService composeService,
            ComposeSelectionService composeSelectionService,
            ComposeApplyRunnerService composeApplyRunnerService,
            ImageCropService imageCropService,
            OcrService ocrService,
            ComposeSessionService composeSessionService,
            HwpxTableExtractor hwpxTableExtractor
    ) {
        this.composeService = composeService;
        this.composeSelectionService = composeSelectionService;
        this.composeApplyRunnerService = composeApplyRunnerService;
        this.imageCropService = imageCropService;
        this.ocrService = ocrService;
        this.composeSessionService = composeSessionService;
        this.hwpxTableExtractor = hwpxTableExtractor;
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

    @PostMapping("/apply")
    public ResponseEntity<ComposeRunApplyResponse> apply(
            @RequestBody ComposeRunApplyRequest request
    ) {
        ComposeRunApplyResponse response = composeApplyRunnerService.run(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/ocr")
    public ResponseEntity<?> ocr(@RequestBody Map<String, Object> req) {
        String imagePath = (String) req.get("imagePath");

        int x = (int) req.get("x");
        int y = (int) req.get("y");
        int w = (int) req.get("width");
        int h = (int) req.get("height");

        File image = new File(imagePath);
        File cropped = imageCropService.crop(image, x, y, w, h);
        String text = ocrService.read(cropped);

        return ResponseEntity.ok(Map.of(
                "text", text
        ));
    }

    @GetMapping("/paragraphs/{sessionId}")
    public ResponseEntity<?> getParagraphs(
            @PathVariable String sessionId,
            @RequestParam String side,
            @RequestParam int page
    ) {
        var session = composeSessionService.get(sessionId);

        if (session == null) {
            throw new IllegalArgumentException("session not found");
        }

        File file = side.equals("template")
                ? session.getTemplatePath().toFile()
                : session.getSourcePath().toFile();

        var paragraphs = composeService.extractFromHwpx(file);

        // 현재는 페이지값이 null일 수 있어서 느슨하게 필터
        var result = paragraphs.stream()
                .filter(p -> p.getPage() == null || p.getPage() == page)
                .toList();

        return ResponseEntity.ok(result);
    }

    // 추가:
    // 표 목록 조회 API
    @GetMapping("/tables/{sessionId}")
    public ResponseEntity<?> getTables(
            @PathVariable String sessionId,
            @RequestParam String side,
            @RequestParam int page
    ) {
        var session = composeSessionService.get(sessionId);

        if (session == null) {
            throw new IllegalArgumentException("session not found");
        }

        File file = side.equals("template")
                ? session.getTemplatePath().toFile()
                : session.getSourcePath().toFile();

        var tables = hwpxTableExtractor.extract(file);

        var result = tables.stream()
                .filter(t -> t.getPage() == null || t.getPage() == page)
                .toList();

        return ResponseEntity.ok(result);
    }
}