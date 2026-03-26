package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRunApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRunApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import com.khwajeong.hwpx_saas.compose.model.ComposeSelection;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComposeApplyRunnerService {

    private final ComposeSelectionService composeSelectionService;
    private final ComposeService composeService;
    private final ComposeSessionService composeSessionService;
    private final TextMatchService textMatchService;
    private final HwpxStyleExtractor hwpxStyleExtractor;

    public ComposeApplyRunnerService(
            ComposeSelectionService composeSelectionService,
            ComposeService composeService,
            ComposeSessionService composeSessionService,
            TextMatchService textMatchService,
            HwpxStyleExtractor hwpxStyleExtractor
    ) {
        this.composeSelectionService = composeSelectionService;
        this.composeService = composeService;
        this.composeSessionService = composeSessionService;
        this.textMatchService = textMatchService;
        this.hwpxStyleExtractor = hwpxStyleExtractor;
    }

    public ComposeRunApplyResponse run(ComposeRunApplyRequest request) {
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

        // 수정: template 실제 스타일 사용
        StyleProfileDto templateStyle =
                hwpxStyleExtractor.extractStyle(
                        new HwpxStyleExtractor.FileWrapper(session.getTemplatePath().toFile()),
                        templateSample.getParaPrIdRef(),
                        templateSample.getCharPrIdRef()
                );

        ComposeApplyRequest applyRequest = new ComposeApplyRequest();
        applyRequest.setTemplateHeading1Style(templateStyle);
        applyRequest.setSourceHeading1Sample(sourceSample);
        applyRequest.setSourceParagraphs(sourceParagraphs);

        ComposeApplyResponse applyResponse = composeService.applyHeading1Style(applyRequest);

        ComposeRunApplyResponse response = new ComposeRunApplyResponse();
        response.setSessionId(request.getSessionId());
        response.setLabel(request.getLabel());
        response.setTemplateSelectionSummary(
                "template page=" + templateSelection.getPage()
                        + ", label=" + templateSelection.getLabel()
                        + ", ocrText=" + templateOcrText
                        + ", matchedParagraphId=" + templateSample.getParagraphId()
                        + ", matchedText=" + templateSample.getText()
                        + ", fontFamily=" + templateStyle.getFontFamily()   // 추가: 검증용
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

        return response;
    }
}