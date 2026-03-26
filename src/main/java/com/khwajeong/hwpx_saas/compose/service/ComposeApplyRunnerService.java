package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRunApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRunApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import com.khwajeong.hwpx_saas.compose.model.ComposeSelection;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class ComposeApplyRunnerService {

    private final ComposeSelectionService composeSelectionService;
    private final ComposeService composeService;
    private final ComposeSessionService composeSessionService;
    private final HwpxStyleExtractor hwpxStyleExtractor;
    private final ComposeHwpxWriterService composeHwpxWriterService;

    public ComposeApplyRunnerService(
            ComposeSelectionService composeSelectionService,
            ComposeService composeService,
            ComposeSessionService composeSessionService,
            HwpxStyleExtractor hwpxStyleExtractor,
            ComposeHwpxWriterService composeHwpxWriterService
    ) {
        this.composeSelectionService = composeSelectionService;
        this.composeService = composeService;
        this.composeSessionService = composeSessionService;
        this.hwpxStyleExtractor = hwpxStyleExtractor;
        this.composeHwpxWriterService = composeHwpxWriterService;
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

            // =========================
            // 핵심 변경:
            // OCR 텍스트 매칭 대신 paragraphId 직접 사용
            // =========================
            ParagraphNodeDto templateSample =
                    findByParagraphId(templateParagraphs, templateSelection.getParagraphId());

            ParagraphNodeDto sourceSample =
                    findByParagraphId(sourceParagraphs, sourceSelection.getParagraphId());

            StyleProfileDto templateStyle =
                    hwpxStyleExtractor.extractStyle(
                            new HwpxStyleExtractor.FileWrapper(session.getTemplatePath().toFile()),
                            templateSample.getParaPrIdRef(),
                            templateSample.getCharPrIdRef()
                    );

            String normalizedFontPolicy = normalizeFontPolicy(request.getFontPolicy());
            templateStyle.setFontFamily(
                    resolveFontFamilyByPolicy(templateStyle.getFontFamily(), normalizedFontPolicy)
            );

            ComposeApplyRequest applyRequest = new ComposeApplyRequest();
            applyRequest.setTemplateHeading1Style(templateStyle);
            applyRequest.setSourceHeading1Sample(sourceSample);
            applyRequest.setSourceParagraphs(sourceParagraphs);

            ComposeApplyResponse applyResponse = composeService.applyHeading1Style(applyRequest);

            Path outputPath = composeHwpxWriterService.writeStyledCopy(
                    session.getSourcePath(),
                    templateSample.getParaPrIdRef(),
                    templateSample.getCharPrIdRef(),
                    applyResponse.getMatchedParagraphIds()
            );

            String fileId = outputPath.getFileName().toString();

            ComposeRunApplyResponse response = new ComposeRunApplyResponse();
            response.setSessionId(request.getSessionId());
            response.setLabel(request.getLabel());
            response.setTemplateSelectionSummary(
                    "template page=" + templateSelection.getPage()
                            + ", label=" + templateSelection.getLabel()
                            + ", paragraphId=" + templateSelection.getParagraphId()
                            + ", matchedParagraphId=" + templateSample.getParagraphId()
                            + ", matchedText=" + templateSample.getText()
                            + ", fontFamily=" + templateStyle.getFontFamily()
                            + ", fontPolicy=" + normalizedFontPolicy
            );
            response.setSourceSelectionSummary(
                    "source page=" + sourceSelection.getPage()
                            + ", label=" + sourceSelection.getLabel()
                            + ", paragraphId=" + sourceSelection.getParagraphId()
                            + ", matchedParagraphId=" + sourceSample.getParagraphId()
                            + ", matchedText=" + sourceSample.getText()
            );
            response.setMatchedParagraphIds(applyResponse.getMatchedParagraphIds());
            response.setStyledParagraphs(applyResponse.getStyledParagraphs());
            response.setOutputPath(outputPath.toString());
            response.setFileId(fileId);
            response.setDownloadUrl("/api/v1/compose/files/" + fileId);

            return response;

        } catch (Exception e) {
            System.out.println("=== [COMPOSE APPLY ERROR] ===");
            e.printStackTrace();
            throw new RuntimeException("compose apply failed: " + e.getMessage(), e);
        }
    }

    // =========================
    // 추가:
    // paragraphId 직접 찾기
    // 이제 textMatchService 대신 이걸 사용
    // =========================
    private ParagraphNodeDto findByParagraphId(List<ParagraphNodeDto> paragraphs, String paragraphId) {
        if (paragraphId == null || paragraphId.isBlank()) {
            throw new IllegalArgumentException("paragraphId is required");
        }

        for (ParagraphNodeDto p : paragraphs) {
            if (paragraphId.equals(p.getParagraphId())) {
                return p;
            }
        }

        throw new IllegalArgumentException("paragraph not found: " + paragraphId);
    }

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

    private String resolveFontFamilyByPolicy(String originalFontFamily, String fontPolicy) {
        if (!"safe".equals(fontPolicy)) {
            return originalFontFamily;
        }

        return applySafeFallback(originalFontFamily);
    }

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