package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRunApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRunApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import com.khwajeong.hwpx_saas.compose.model.ComposeSelection;
import com.khwajeong.hwpx_saas.core.ExportService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComposeApplyRunnerService {

    private final ComposeSelectionService composeSelectionService;
    private final ComposeService composeService;
    private final ComposeSessionService composeSessionService;
    private final TextMatchService textMatchService;
    private final HwpxStyleExtractor hwpxStyleExtractor;
    private final ExportService exportService;

    public ComposeApplyRunnerService(
            ComposeSelectionService composeSelectionService,
            ComposeService composeService,
            ComposeSessionService composeSessionService,
            TextMatchService textMatchService,
            HwpxStyleExtractor hwpxStyleExtractor,
            ExportService exportService
    ) {
        this.composeSelectionService = composeSelectionService;
        this.composeService = composeService;
        this.composeSessionService = composeSessionService;
        this.textMatchService = textMatchService;
        this.hwpxStyleExtractor = hwpxStyleExtractor;
        this.exportService = exportService;
    }

    public ComposeRunApplyResponse run(ComposeRunApplyRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("request is null");
            }
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                throw new IllegalArgumentException("sessionId is required");
            }
            if (request.getLabel() == null || request.getLabel().isBlank()) {
                throw new IllegalArgumentException("label is required");
            }

            ComposeSessionService.SessionData session =
                    composeSessionService.get(request.getSessionId());

            if (session == null) {
                throw new IllegalArgumentException("session not found: " + request.getSessionId());
            }

            ComposeSelection templateSelection =
                    composeSelectionService.findFirstBySessionIdAndSideAndLabel(
                            request.getSessionId(), "template", request.getLabel()
                    );

            ComposeSelection sourceSelection =
                    composeSelectionService.findFirstBySessionIdAndSideAndLabel(
                            request.getSessionId(), "source", request.getLabel()
                    );

            if (templateSelection == null) {
                throw new IllegalArgumentException("template selection not found for label: " + request.getLabel());
            }
            if (sourceSelection == null) {
                throw new IllegalArgumentException("source selection not found for label: " + request.getLabel());
            }

            List<ParagraphNodeDto> templateParagraphs =
                    composeService.extractFromHwpx(session.getTemplatePath().toFile());

            List<ParagraphNodeDto> sourceParagraphs =
                    composeService.extractFromHwpx(session.getSourcePath().toFile());

            if (templateParagraphs == null || templateParagraphs.isEmpty()) {
                throw new IllegalArgumentException("template paragraphs not extracted");
            }
            if (sourceParagraphs == null || sourceParagraphs.isEmpty()) {
                throw new IllegalArgumentException("source paragraphs not extracted");
            }

            String templateOcrText = templateSelection.getOcrText();
            String sourceOcrText = sourceSelection.getOcrText();

            ParagraphNodeDto templateSample =
                    textMatchService.findBestMatch(templateParagraphs, templateOcrText);

            ParagraphNodeDto sourceSample =
                    textMatchService.findBestMatch(sourceParagraphs, sourceOcrText);

            // =========================
            // 템플릿 문단의 실제 스타일 추출
            // =========================
            StyleProfileDto templateStyle =
                    hwpxStyleExtractor.extractStyle(
                            new HwpxStyleExtractor.FileWrapper(session.getTemplatePath().toFile()),
                            templateSample.getParaPrIdRef(),
                            templateSample.getCharPrIdRef()
                    );

            // =========================
            // 추가:
            // fontPolicy 반영
            // - original : 그대로
            // - safe     : fallback 폰트로 치환
            // =========================
            String normalizedFontPolicy = normalizeFontPolicy(request.getFontPolicy());
            templateStyle.setFontFamily(
                    resolveFontFamilyByPolicy(templateStyle.getFontFamily(), normalizedFontPolicy)
            );

            ComposeApplyRequest applyRequest = new ComposeApplyRequest();
            applyRequest.setTemplateHeading1Style(templateStyle);
            applyRequest.setSourceHeading1Sample(sourceSample);
            applyRequest.setSourceParagraphs(sourceParagraphs);

            ComposeApplyResponse applyResponse = composeService.applyHeading1Style(applyRequest);

            // =========================
            // render 요청 생성
            // =========================
            Map<String, Object> renderRequest = buildRenderRequest(applyResponse);

            byte[] resultBytes = exportService.renderHwpx(
                    Files.readAllBytes(session.getTemplatePath()),
                    renderRequest
            );

            Path outputPath = Files.createTempFile("compose-output-", ".hwpx");
            Files.write(outputPath, resultBytes);

            ComposeRunApplyResponse response = new ComposeRunApplyResponse();
            response.setSessionId(request.getSessionId());
            response.setLabel(request.getLabel());
            response.setTemplateSelectionSummary(
                    "template page=" + templateSelection.getPage()
                            + ", label=" + templateSelection.getLabel()
                            + ", ocrText=" + templateOcrText
                            + ", matchedParagraphId=" + templateSample.getParagraphId()
                            + ", matchedText=" + templateSample.getText()
                            + ", fontFamily=" + templateStyle.getFontFamily()
                            + ", fontPolicy=" + normalizedFontPolicy
            );
            response.setSourceSelectionSummary(
                    "source page=" + sourceSelection.getPage()
                            + ", label=" + sourceSelection.getLabel()
                            + ", ocrText=" + sourceOcrText
                            + ", matchedParagraphId=" + sourceSample.getParagraphId()
                            + ", matchedText=" + sourceSample.getText()
            );
            response.setMatchedParagraphIds(applyResponse.getMatchedParagraphIds());
            response.setStyledParagraphs(applyResponse.getStyledParagraphs());
            response.setOutputPath(outputPath.toString());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("compose apply/export failed", e);
        }
    }

    // =========================
    // 추가:
    // ExportService 에 넘길 request body 생성
    // =========================
    private Map<String, Object> buildRenderRequest(ComposeApplyResponse applyResponse) {
        Map<String, Object> request = new HashMap<>();
        List<Map<String, Object>> elements = new ArrayList<>();

        if (applyResponse == null || applyResponse.getStyledParagraphs() == null) {
            request.put("elements", elements);
            return request;
        }

        for (ParagraphNodeDto p : applyResponse.getStyledParagraphs()) {
            Map<String, Object> element = new HashMap<>();
            element.put("placeholder", p.getParagraphId());
            element.put("type", "text");
            element.put("content", p.getText());

            Map<String, Object> style = new HashMap<>();

            style.put("font_family", p.getFontFamily());

            if (p.getFontSize() != null) {
                style.put("font_size", p.getFontSize());
            }
            if (p.getBold() != null) {
                style.put("bold", p.getBold());
            }
            if (p.getItalic() != null) {
                style.put("italic", p.getItalic());
            }
            if (p.getUnderline() != null) {
                style.put("underline", p.getUnderline());
            }
            if (p.getColor() != null) {
                style.put("color", p.getColor());
            }
            if (p.getAlign() != null) {
                style.put("align", p.getAlign());
            }
            if (p.getLineSpacing() != null) {
                style.put("line_spacing", p.getLineSpacing());
            }
            if (p.getSpaceBefore() != null) {
                style.put("space_before", p.getSpaceBefore());
            }
            if (p.getSpaceAfter() != null) {
                style.put("space_after", p.getSpaceAfter());
            }

            element.put("style", style);
            elements.add(element);
        }

        request.put("elements", elements);
        return request;
    }

    // =========================
    // 추가:
    // fontPolicy 정규화
    // =========================
    private String normalizeFontPolicy(String fontPolicy) {
        if (fontPolicy == null || fontPolicy.isBlank()) {
            return "original";
        }

        String v = fontPolicy.trim().toLowerCase();
        if ("safe".equals(v)) {
            return "safe";
        }
        return "original";
    }

    // =========================
    // 추가:
    // 폰트 정책 적용
    // original: 원본 폰트 그대로
    // safe    : 안전 폰트로 치환
    // =========================
    private String resolveFontFamilyByPolicy(String originalFontFamily, String fontPolicy) {
        if (!"safe".equals(fontPolicy)) {
            return originalFontFamily;
        }

        return applySafeFallback(originalFontFamily);
    }

    // =========================
    // 추가:
    // 안전 폰트 fallback 규칙
    // - 필요하면 계속 확장 가능
    // =========================
    private String applySafeFallback(String fontFamily) {
        if (fontFamily == null || fontFamily.isBlank()) {
            return "맑은 고딕";
        }

        String f = fontFamily.trim();

        if (f.equalsIgnoreCase("Pretendard")
                || f.equalsIgnoreCase("Pretendard Black")
                || f.equalsIgnoreCase("Pretendard ExtraBold")
                || f.equalsIgnoreCase("Pretendard Bold")) {
            return "맑은 고딕";
        }

        if (f.equalsIgnoreCase("Roboto")) {
            return "Arial";
        }

        if (f.equalsIgnoreCase("NanumGothic")
                || f.equals("나눔고딕")) {
            return "나눔고딕";
        }

        return f;
    }
}