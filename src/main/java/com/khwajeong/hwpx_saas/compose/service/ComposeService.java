package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeApplyResponse;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import com.khwajeong.hwpx_saas.compose.model.ParagraphNode;
import com.khwajeong.hwpx_saas.compose.model.StyleProfile;
import org.springframework.stereotype.Service;

import com.khwajeong.hwpx_saas.compose.dto.ComposePreviewResponse;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class ComposeService {

    private final Heading1Classifier heading1Classifier;
    private final HwpxParagraphExtractor extractor;
    private final ComposeFileStorageService composeFileStorageService;
    private final ComposeSessionService composeSessionService;

    public ComposeService(
            Heading1Classifier heading1Classifier,
            ComposeFileStorageService composeFileStorageService,
            ComposeSessionService composeSessionService,
            HwpxParagraphExtractor extractor
    ) {
        this.heading1Classifier = heading1Classifier;
        this.composeFileStorageService = composeFileStorageService;
        this.composeSessionService = composeSessionService;
        this.extractor = extractor;
    }

    public ComposeApplyResponse applyHeading1Style(ComposeApplyRequest request) {
        validate(request);

        StyleProfile templateStyle = toStyleProfile(request.getTemplateHeading1Style());
        ParagraphNode sourceSample = toParagraphNode(request.getSourceHeading1Sample());

        List<String> matchedIds = new ArrayList<>();
        List<ParagraphNodeDto> result = new ArrayList<>();

        for (ParagraphNodeDto dto : request.getSourceParagraphs()) {
            ParagraphNode node = toParagraphNode(dto);

            if (heading1Classifier.isHeading1(node, sourceSample)) {
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
        node.setColor(style.getColor());
        node.setAlign(style.getAlign());
        node.setSpaceBefore(style.getSpaceBefore());
        node.setSpaceAfter(style.getSpaceAfter());
        node.setLineSpacing(style.getLineSpacing());
    }

    private StyleProfile toStyleProfile(StyleProfileDto dto) {
        StyleProfile style = new StyleProfile();
        style.setFontSize(dto.getFontSize());
        style.setBold(dto.getBold());
        style.setColor(dto.getColor());
        style.setAlign(dto.getAlign());
        style.setSpaceBefore(dto.getSpaceBefore());
        style.setSpaceAfter(dto.getSpaceAfter());
        style.setLineSpacing(dto.getLineSpacing());
        return style;
    }

    private ParagraphNode toParagraphNode(ParagraphNodeDto dto) {
        ParagraphNode node = new ParagraphNode();
        node.setParagraphId(dto.getParagraphId());
        node.setPage(dto.getPage());
        node.setText(dto.getText());
        node.setFontSize(dto.getFontSize());
        node.setBold(dto.getBold());
        node.setColor(dto.getColor());
        node.setAlign(dto.getAlign());
        node.setSpaceBefore(dto.getSpaceBefore());
        node.setSpaceAfter(dto.getSpaceAfter());
        node.setLineSpacing(dto.getLineSpacing());
        node.setSingleLine(dto.getSingleLine());
        return node;
    }

    private ParagraphNodeDto toDto(ParagraphNode node) {
        ParagraphNodeDto dto = new ParagraphNodeDto();
        dto.setParagraphId(node.getParagraphId());
        dto.setPage(node.getPage());
        dto.setText(node.getText());
        dto.setFontSize(node.getFontSize());
        dto.setBold(node.getBold());
        dto.setColor(node.getColor());
        dto.setAlign(node.getAlign());
        dto.setSpaceBefore(node.getSpaceBefore());
        dto.setSpaceAfter(node.getSpaceAfter());
        dto.setLineSpacing(node.getLineSpacing());
        dto.setSingleLine(node.getSingleLine());
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

            // 기본값 (나중에 스타일 읽기 추가)
            dto.setFontSize(12);
            dto.setBold(false);
            dto.setAlign("left");
            dto.setSpaceBefore(0);
            dto.setSpaceAfter(0);
            dto.setLineSpacing(160);

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

        // ⭐ 핵심: 세션에 파일 경로 저장
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