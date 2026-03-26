package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.*;
import com.khwajeong.hwpx_saas.compose.model.ComposeSelection;
import com.khwajeong.hwpx_saas.compose.service.TextMatchService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ComposeApplyRunnerService {

    private final ComposeSelectionService composeSelectionService;
    private final ComposeService composeService;
    private final ComposeSessionService composeSessionService;
    private final TextMatchService textMatchService;

    public ComposeApplyRunnerService(
            ComposeSelectionService composeSelectionService,
            ComposeService composeService,
            ComposeSessionService composeSessionService,
            TextMatchService textMatchService
    ) {
        this.composeSelectionService = composeSelectionService;
        this.composeService = composeService;
        this.composeSessionService = composeSessionService;
        this.textMatchService = textMatchService;
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

    // 1. 세션 가져오기
    ComposeSessionService.SessionData session =
            composeSessionService.get(request.getSessionId());

    if (session == null) {
        throw new IllegalArgumentException("session not found: " + request.getSessionId());
    }

    // 2. 사용자가 저장한 selection 가져오기
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

    // 3. template/source 문단 추출
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

    // 4. OCR 텍스트로 양쪽 샘플 문단 찾기
    String templateOcrText = templateSelection.getOcrText();
    String sourceOcrText = sourceSelection.getOcrText();

    ParagraphNodeDto templateSample =
            textMatchService.findBestMatch(templateParagraphs, templateOcrText);

    ParagraphNodeDto sourceSample =
            textMatchService.findBestMatch(sourceParagraphs, sourceOcrText);

    // 5. 아직은 template 실제 스타일 추출 전이라 임시 스타일 유지
    //    단, 구조상 templateSample을 이미 찾았으므로 다음 단계에서 여기만 바꾸면 됨
    StyleProfileDto templateStyle = new StyleProfileDto();
    templateStyle.setFontSize(20);
    templateStyle.setBold(true);
    templateStyle.setColor("#111111");
    templateStyle.setAlign("left");
    templateStyle.setSpaceBefore(12);
    templateStyle.setSpaceAfter(6);
    templateStyle.setLineSpacing(160);

    // 6. apply 실행
    ComposeApplyRequest applyRequest = new ComposeApplyRequest();
    applyRequest.setTemplateHeading1Style(templateStyle);
    applyRequest.setSourceHeading1Sample(sourceSample);
    applyRequest.setSourceParagraphs(sourceParagraphs);

    ComposeApplyResponse applyResponse = composeService.applyHeading1Style(applyRequest);

    // 7. 응답
    ComposeRunApplyResponse response = new ComposeRunApplyResponse();
    response.setSessionId(request.getSessionId());
    response.setLabel(request.getLabel());
    response.setTemplateSelectionSummary(
            "template page=" + templateSelection.getPage()
                    + ", label=" + templateSelection.getLabel()
                    + ", ocrText=" + templateOcrText
                    + ", matchedParagraphId=" + templateSample.getParagraphId()
                    + ", matchedText=" + templateSample.getText()
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