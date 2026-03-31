package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposePreviewResponse;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import com.khwajeong.hwpx_saas.compose.model.ParagraphNode;
import com.khwajeong.hwpx_saas.compose.model.StyleProfile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ComposeService {

    private final Heading1Classifier heading1Classifier;
    private final HwpxParagraphExtractor extractor;
    private final ComposeFileStorageService composeFileStorageService;
    private final ComposeSessionService composeSessionService;
    private final HwpxStyleExtractor hwpxStyleExtractor;

    public ComposeService(
            Heading1Classifier heading1Classifier,
            ComposeFileStorageService composeFileStorageService,
            ComposeSessionService composeSessionService,
            HwpxParagraphExtractor extractor,
            HwpxStyleExtractor hwpxStyleExtractor
    ) {
        this.heading1Classifier = heading1Classifier;
        this.composeFileStorageService = composeFileStorageService;
        this.composeSessionService = composeSessionService;
        this.extractor = extractor;
        this.hwpxStyleExtractor = hwpxStyleExtractor;
    }

    public ComposeApplyResponse applyHeading1Style(ComposeApplyRequest request) {
        validate(request);

        StyleProfile templateStyle = toStyleProfile(request.getTemplateHeading1Style());
        ParagraphNode sourceSample = toParagraphNode(request.getSourceHeading1Sample());

        List<String> matchedIds = new ArrayList<>();
        List<ParagraphNodeDto> result = new ArrayList<>();

        for (ParagraphNodeDto dto : request.getSourceParagraphs()) {
            ParagraphNode node = toParagraphNode(dto);

            /* 260331
            if (heading1Classifier.isHeading1(node, sourceSample)) {
                matchedIds.add(node.getParagraphId());
                applyStyle(node, templateStyle);
            }
            */
           if (node.getParagraphId().equals(sourceSample.getParagraphId())) {
                matchedIds.add(node.getParagraphId());
                applyStyle(node, templateStyle);
            }

            result.add(toDto(node));
        }

        return new ComposeApplyResponse(matchedIds, result);
    }

    private void validate(ComposeApplyRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }
        if (request.getTemplateHeading1Style() == null) {
            throw new IllegalArgumentException("templateHeading1Style is required");
        }
        if (request.getSourceHeading1Sample() == null) {
            throw new IllegalArgumentException("sourceHeading1Sample is required");
        }
        if (request.getSourceParagraphs() == null || request.getSourceParagraphs().isEmpty()) {
            throw new IllegalArgumentException("sourceParagraphs is required");
        }
    }

    private void applyStyle(ParagraphNode node, StyleProfile style) {
        node.setFontSize(style.getFontSize());
        node.setBold(style.getBold());
        node.setItalic(style.getItalic());         // 추가: italic 적용
        node.setUnderline(style.getUnderline());   // 추가: underline 적용
        node.setColor(style.getColor());
        node.setAlign(style.getAlign());
        node.setSpaceBefore(style.getSpaceBefore());
        node.setSpaceAfter(style.getSpaceAfter());
        node.setLineSpacing(style.getLineSpacing());
        node.setFontFamily(style.getFontFamily()); // 추가: fontFamily 적용
    }

    private StyleProfile toStyleProfile(StyleProfileDto dto) {
        StyleProfile style = new StyleProfile();
        style.setFontSize(dto.getFontSize());
        style.setBold(dto.getBold());
        style.setItalic(dto.getItalic());           // 추가
        style.setUnderline(dto.getUnderline());     // 추가
        style.setColor(dto.getColor());
        style.setAlign(dto.getAlign());
        style.setSpaceBefore(dto.getSpaceBefore());
        style.setSpaceAfter(dto.getSpaceAfter());
        style.setLineSpacing(dto.getLineSpacing());
        style.setFontFamily(dto.getFontFamily());   // 추가
        return style;
    }

    private ParagraphNode toParagraphNode(ParagraphNodeDto dto) {
        ParagraphNode node = new ParagraphNode();
        node.setParagraphId(dto.getParagraphId());
        node.setPage(dto.getPage());
        node.setText(dto.getText());
        node.setFontSize(dto.getFontSize());
        node.setBold(dto.getBold());
        node.setItalic(dto.getItalic());            // 추가
        node.setUnderline(dto.getUnderline());      // 추가
        node.setColor(dto.getColor());
        node.setAlign(dto.getAlign());
        node.setSpaceBefore(dto.getSpaceBefore());
        node.setSpaceAfter(dto.getSpaceAfter());
        node.setLineSpacing(dto.getLineSpacing());
        node.setSingleLine(dto.getSingleLine());
        node.setFontFamily(dto.getFontFamily());    // 추가
        return node;
    }

    private ParagraphNodeDto toDto(ParagraphNode node) {
        ParagraphNodeDto dto = new ParagraphNodeDto();
        dto.setParagraphId(node.getParagraphId());
        dto.setPage(node.getPage());
        dto.setText(node.getText());
        dto.setFontSize(node.getFontSize());
        dto.setBold(node.getBold());
        dto.setItalic(node.getItalic());            // 추가
        dto.setUnderline(node.getUnderline());      // 추가
        dto.setColor(node.getColor());
        dto.setAlign(node.getAlign());
        dto.setSpaceBefore(node.getSpaceBefore());
        dto.setSpaceAfter(node.getSpaceAfter());
        dto.setLineSpacing(node.getLineSpacing());
        dto.setSingleLine(node.getSingleLine());
        dto.setFontFamily(node.getFontFamily());    // 추가
        return dto;
    }

    public List<ParagraphNodeDto> extractFromHwpx(File file) {
        List<HwpxParagraphExtractor.SimpleParagraph> list = extractor.extract(file);

        List<ParagraphNodeDto> result = new ArrayList<>();

        for (HwpxParagraphExtractor.SimpleParagraph sp : list) {
            ParagraphNodeDto dto = new ParagraphNodeDto();
            dto.setParagraphId(sp.getParagraphId());
            dto.setText(sp.getText());
            dto.setSingleLine(sp.isSingleLine());

            dto.setParaPrIdRef(sp.getParaPrIdRef());
            dto.setCharPrIdRef(sp.getCharPrIdRef());

            // 수정: 실제 스타일값 읽기
            StyleProfileDto style = hwpxStyleExtractor.extractStyle(
                    new HwpxStyleExtractor.FileWrapper(file),
                    sp.getParaPrIdRef(),
                    sp.getCharPrIdRef()
            );

            dto.setFontSize(style.getFontSize());
            dto.setBold(style.getBold());
            dto.setItalic(style.getItalic());
            dto.setUnderline(style.getUnderline());
            dto.setColor(style.getColor());
            dto.setAlign(style.getAlign());
            dto.setSpaceBefore(style.getSpaceBefore());
            dto.setSpaceAfter(style.getSpaceAfter());
            dto.setLineSpacing(style.getLineSpacing());
            dto.setFontFamily(style.getFontFamily());   // 추가: 실제 fontFamily 반영

            // 추가:
            // 임시 좌표 생성 로직
            // 지금은 paragraph 순서 기반으로 페이지에 세로 배치
            // 추가: 스타일 기반 레이아웃 계산
            int index = result.size();

            int baseLeft = 56;
            int baseWidth = 420;

            // 기본값
            int fontSize = dto.getFontSize() != null ? dto.getFontSize() : 12;
            boolean bold = Boolean.TRUE.equals(dto.getBold());

            // 제목 판단 (간단 rule)
            boolean isHeading = fontSize >= 14 || bold;

            // 줄 높이 계산
            int lineHeight = (int) (fontSize * 1.6);

            // 문단 간 간격
            int spacingBefore = isHeading ? 18 : 6;
            int spacingAfter = isHeading ? 10 : 4;

            // 누적 y 계산
            int prevBottom = 0;
            if (!result.isEmpty()) {
                var prev = result.get(result.size() - 1);
                prevBottom = (int)((prev.getY() != null ? prev.getY() : 0)
                    + (prev.getHeight() != null ? prev.getHeight() : 34));
            }

            // 현재 y 위치
            int top = prevBottom + spacingBefore;

            // 페이지 넘김 처리
            int pageHeightLimit = 680;
            int currentPage = (top / pageHeightLimit) + 1;
            int normalizedTop = top % pageHeightLimit;

            // height 계산 (멀티라인 고려)
            int textLength = dto.getText() != null ? dto.getText().length() : 0;
            int estimatedLines = Math.max(1, textLength / 20);
            int height = estimatedLines * lineHeight + spacingAfter;

            // 적용
            dto.setPage(currentPage);
            dto.setX((double) baseLeft);
            dto.setY((double) normalizedTop);
            dto.setWidth((double) baseWidth);
            dto.setHeight((double) height);

            result.add(dto);
        }

        return result;
    }

    public ComposePreviewResponse createPreviewSession(
            MultipartFile templateFile,
            MultipartFile sourceFile
    ) {
        Path templatePath = composeFileStorageService.save(templateFile, "template");
        Path sourcePath = composeFileStorageService.save(sourceFile, "source");

        String sessionId = composeSessionService.createSessionId();
        composeSessionService.create(sessionId, templatePath, sourcePath);

        ComposePreviewResponse response = new ComposePreviewResponse();
        response.setSessionId(sessionId);
        response.setTemplateOriginalFilename(templateFile.getOriginalFilename());
        response.setSourceOriginalFilename(sourceFile.getOriginalFilename());
        response.setTemplateSavedPath(templatePath.toString());
        response.setSourceSavedPath(sourcePath.toString());

        return response;
    }
}