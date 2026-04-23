package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.*;
import com.khwajeong.hwpx_saas.compose.model.ComposeSelection;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComposeApplyRunnerService {

    private final ComposeSelectionService composeSelectionService;
    private final ComposeService composeService;
    private final ComposeSessionService composeSessionService;
    private final HwpxStyleExtractor hwpxStyleExtractor;
    private final ComposeTextStyleApplyService composeTextStyleApplyService;
    private final HwpxTableExtractor hwpxTableExtractor;
    private final ComposeTableStyleApplyService composeTableStyleApplyService;
    private final IconClassifier iconClassifier;

    public ComposeApplyRunnerService(
            ComposeSelectionService composeSelectionService,
            ComposeService composeService,
            ComposeSessionService composeSessionService,
            HwpxStyleExtractor hwpxStyleExtractor,
            ComposeTextStyleApplyService composeTextStyleApplyService,
            HwpxTableExtractor hwpxTableExtractor,
            ComposeTableStyleApplyService composeTableStyleApplyService,
            IconClassifier iconClassifier
    ) {
        this.composeSelectionService = composeSelectionService;
        this.composeService = composeService;
        this.composeSessionService = composeSessionService;
        this.hwpxStyleExtractor = hwpxStyleExtractor;
        this.composeTextStyleApplyService = composeTextStyleApplyService;
        this.hwpxTableExtractor = hwpxTableExtractor;
        this.composeTableStyleApplyService = composeTableStyleApplyService;
        this.iconClassifier = iconClassifier;
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

            if ("table".equalsIgnoreCase(templateSelection.getBlockType())
                    && "oneline_box".equalsIgnoreCase(templateSelection.getLabel())) {
                return runOnelineBoxApply(request, session, templateSelection, sourceSelection, session.getSourcePath());
            }

            if ("table".equalsIgnoreCase(templateSelection.getBlockType())
                    || "table".equalsIgnoreCase(sourceSelection.getBlockType())) {
                return runTableApply(request, session, templateSelection, sourceSelection, session.getSourcePath());
            }

            return runParagraphApply(request, session, templateSelection, sourceSelection, session.getSourcePath());

        } catch (Exception e) {
            System.out.println("=== [COMPOSE APPLY ERROR] ===");
            e.printStackTrace();
            throw new RuntimeException("compose apply failed: " + e.getMessage(), e);
        }
    }

    public ComposeRunApplyResponse runAll(ComposeRunApplyAllRequest request) {
        try {
            if (request == null) {
                throw new IllegalArgumentException("request is null");
            }
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                throw new IllegalArgumentException("sessionId is required");
            }

            ComposeSessionService.SessionData session =
                    composeSessionService.get(request.getSessionId());

            if (session == null) {
                throw new IllegalArgumentException("session not found: " + request.getSessionId());
            }

            ComposeSelectionsResponse selectionsResponse =
                    composeSelectionService.getBySessionId(request.getSessionId());

            List<ComposeSelectionResponse> allSelections =
                    selectionsResponse.getSelections() == null
                            ? List.of()
                            : selectionsResponse.getSelections();

            if (allSelections.isEmpty()) {
                throw new IllegalArgumentException("no selections found in this session");
            }

            Map<String, ComposeSelectionResponse> templateMap = allSelections.stream()
                    .filter(s -> "template".equalsIgnoreCase(s.getSide()))
                    .collect(Collectors.toMap(
                            ComposeSelectionResponse::getLabel,
                            s -> s,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            Map<String, ComposeSelectionResponse> sourceMap = allSelections.stream()
                    .filter(s -> "source".equalsIgnoreCase(s.getSide()))
                    .collect(Collectors.toMap(
                            ComposeSelectionResponse::getLabel,
                            s -> s,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            List<String> applyOrder = List.of(
                    "heading1",
                    "heading2",
                    "heading3",
                    "body",
                    "icon",
                    "caption",
                    "oneline_box",
                    "table"
            );

            Path workingSourcePath = session.getSourcePath();
            List<String> appliedLabels = new ArrayList<>();
            List<String> appliedTargets = new ArrayList<>();

            for (String label : applyOrder) {
                ComposeSelectionResponse templateSel = templateMap.get(label);
                ComposeSelectionResponse sourceSel = sourceMap.get(label);

                if (templateSel == null || sourceSel == null) {
                    continue;
                }

                if ("oneline_box".equalsIgnoreCase(label)
                        && "table".equalsIgnoreCase(templateSel.getBlockType())
                        && !"table".equalsIgnoreCase(sourceSel.getBlockType())) {

                    ComposeRunApplyRequest singleRequest = new ComposeRunApplyRequest();
                    singleRequest.setSessionId(request.getSessionId());
                    singleRequest.setLabel(label);
                    singleRequest.setFontPolicy(request.getFontPolicy());

                    ComposeSelection templateModel = toModel(templateSel);
                    ComposeSelection sourceModel = toModel(sourceSel);

                    ComposeRunApplyResponse singleResponse = runOnelineBoxApply(
                            singleRequest,
                            session,
                            templateModel,
                            sourceModel,
                            workingSourcePath
                    );

                    workingSourcePath = Path.of(singleResponse.getOutputPath());
                    appliedLabels.add(label);
                    appliedTargets.add("oneline_box:" + nvl(sourceSel.getBlockId(), sourceSel.getParagraphId()));
                    continue;
                }

                if ("table".equalsIgnoreCase(templateSel.getBlockType())
                        || "table".equalsIgnoreCase(sourceSel.getBlockType())) {

                    ComposeRunApplyRequest singleRequest = new ComposeRunApplyRequest();
                    singleRequest.setSessionId(request.getSessionId());
                    singleRequest.setLabel(label);
                    singleRequest.setFontPolicy(request.getFontPolicy());

                    ComposeSelection templateModel = toModel(templateSel);
                    ComposeSelection sourceModel = toModel(sourceSel);

                    ComposeRunApplyResponse singleResponse = runTableApply(
                            singleRequest,
                            session,
                            templateModel,
                            sourceModel,
                            workingSourcePath
                    );

                    workingSourcePath = Path.of(singleResponse.getOutputPath());
                    appliedLabels.add(label);
                    appliedTargets.add("table:" + nvl(sourceSel.getBlockId(), sourceSel.getTableId()));
                    continue;
                }

                ComposeRunApplyRequest singleRequest = new ComposeRunApplyRequest();
                singleRequest.setSessionId(request.getSessionId());
                singleRequest.setLabel(label);
                singleRequest.setFontPolicy(request.getFontPolicy());

                ComposeSelection templateModel = toModel(templateSel);
                ComposeSelection sourceModel = toModel(sourceSel);

                ComposeRunApplyResponse singleResponse = runParagraphApply(
                        singleRequest,
                        session,
                        templateModel,
                        sourceModel,
                        workingSourcePath
                );

                workingSourcePath = Path.of(singleResponse.getOutputPath());
                appliedLabels.add(label);
                appliedTargets.add("paragraph:" + nvl(sourceSel.getBlockId(), sourceSel.getParagraphId()));
            }

            if (appliedLabels.isEmpty()) {
                throw new IllegalArgumentException("no applicable label pairs found");
            }

            ComposeRunApplyResponse finalResponse = new ComposeRunApplyResponse();
            finalResponse.setSessionId(request.getSessionId());
            finalResponse.setLabel(String.join(",", appliedLabels));
            finalResponse.setTemplateSelectionSummary("template applied labels = " + appliedLabels);
            finalResponse.setSourceSelectionSummary("source applied targets = " + appliedTargets);
            finalResponse.setMatchedParagraphIds(List.of());
            finalResponse.setStyledParagraphs(List.of());
            finalResponse.setOutputPath(workingSourcePath.toString());
            finalResponse.setFileId(workingSourcePath.getFileName().toString());
            finalResponse.setDownloadUrl("/api/v1/compose/files/" + workingSourcePath.getFileName());

            return finalResponse;

        } catch (Exception e) {
            System.out.println("=== [COMPOSE APPLY-ALL ERROR] ===");
            e.printStackTrace();
            throw new RuntimeException("compose apply-all failed: " + e.getMessage(), e);
        }
    }

    private ComposeRunApplyResponse runParagraphApply(
            ComposeRunApplyRequest request,
            ComposeSessionService.SessionData session,
            ComposeSelection templateSelection,
            ComposeSelection sourceSelection,
            Path sourceHwpxPath
    ) throws Exception {
        List<ParagraphNodeDto> templateParagraphs =
                composeService.extractFromHwpx(session.getTemplatePath().toFile());

        List<ParagraphNodeDto> sourceParagraphs =
                composeService.extractFromHwpx(sourceHwpxPath.toFile());

        if (templateParagraphs == null || templateParagraphs.isEmpty()) {
            throw new IllegalArgumentException("template paragraphs not extracted");
        }
        if (sourceParagraphs == null || sourceParagraphs.isEmpty()) {
            throw new IllegalArgumentException("source paragraphs not extracted");
        }

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

        String prefixToInject = null;
        if ("icon".equalsIgnoreCase(request.getLabel())) {
            prefixToInject = resolveIconPrefix(templateSelection, templateSample);
        }

        Path outputPath = composeTextStyleApplyService.applyTextStyleUsingRenderer(
                sourceHwpxPath,
                templateStyle,
                sourceParagraphs,
                applyResponse.getMatchedParagraphIds(),
                prefixToInject
        );

        String fileId = outputPath.getFileName().toString();

        ComposeRunApplyResponse response = new ComposeRunApplyResponse();
        response.setSessionId(request.getSessionId());
        response.setLabel(request.getLabel());
        response.setTemplateSelectionSummary(
                "template page=" + templateSelection.getPage()
                        + ", label=" + templateSelection.getLabel()
                        + ", paragraphId=" + templateSelection.getParagraphId()
                        + ", blockId=" + nvl(templateSelection.getBlockId(), "")
                        + ", blockType=" + templateSelection.getBlockType()
                        + ", subType=" + nvl(templateSelection.getSubType(), "")
                        + ", sourceKind=" + nvl(templateSelection.getSourceKind(), "")
                        + ", prefix=" + nvl(prefixToInject, "")
        );
        response.setSourceSelectionSummary(
                "source page=" + sourceSelection.getPage()
                        + ", label=" + sourceSelection.getLabel()
                        + ", paragraphId=" + sourceSelection.getParagraphId()
                        + ", blockId=" + nvl(sourceSelection.getBlockId(), "")
                        + ", blockType=" + sourceSelection.getBlockType()
                        + ", subType=" + nvl(sourceSelection.getSubType(), "")
                        + ", sourceKind=" + nvl(sourceSelection.getSourceKind(), "")
        );
        response.setMatchedParagraphIds(applyResponse.getMatchedParagraphIds());
        response.setStyledParagraphs(applyResponse.getStyledParagraphs());
        response.setOutputPath(outputPath.toString());
        response.setFileId(fileId);
        response.setDownloadUrl("/api/v1/compose/files/" + fileId);

        return response;
    }

    private ComposeRunApplyResponse runOnelineBoxApply(
            ComposeRunApplyRequest request,
            ComposeSessionService.SessionData session,
            ComposeSelection templateSelection,
            ComposeSelection sourceSelection,
            Path sourceHwpxPath
    ) throws Exception {

        if (templateSelection.getTableId() == null || templateSelection.getTableId().isBlank()) {
            throw new IllegalArgumentException("template oneline_box tableId is required");
        }
        if (sourceSelection.getParagraphId() == null || sourceSelection.getParagraphId().isBlank()) {
            throw new IllegalArgumentException("source paragraphId is required for oneline_box apply");
        }

        List<TableNodeDto> templateTables = hwpxTableExtractor.extract(session.getTemplatePath().toFile());
        TableNodeDto templateTable = findByTableId(templateTables, templateSelection.getTableId());

        List<ParagraphNodeDto> sourceParagraphs = composeService.extractFromHwpx(sourceHwpxPath.toFile());
        ParagraphNodeDto sourceParagraph = findByParagraphId(sourceParagraphs, sourceSelection.getParagraphId());

        String sourceText = sourceParagraph.getText() == null ? "" : sourceParagraph.getText().trim();

        Path outputPath = composeTableStyleApplyService.applyOnelineBoxToParagraph(
                session.getTemplatePath(),
                templateSelection.getTableId(),
                sourceHwpxPath,
                sourceSelection.getParagraphId(),
                sourceText,
                templateTable
        );

        String fileId = outputPath.getFileName().toString();

        ComposeRunApplyResponse response = new ComposeRunApplyResponse();
        response.setSessionId(request.getSessionId());
        response.setLabel(request.getLabel());
        response.setTemplateSelectionSummary(
                "template page=" + templateSelection.getPage()
                        + ", label=" + templateSelection.getLabel()
                        + ", tableId=" + templateSelection.getTableId()
                        + ", blockId=" + nvl(templateSelection.getBlockId(), "")
                        + ", blockType=" + templateSelection.getBlockType()
                        + ", subType=" + nvl(templateSelection.getSubType(), "")
                        + ", sourceKind=" + nvl(templateSelection.getSourceKind(), "")
        );
        response.setSourceSelectionSummary(
                "source page=" + sourceSelection.getPage()
                        + ", label=" + sourceSelection.getLabel()
                        + ", paragraphId=" + sourceSelection.getParagraphId()
                        + ", blockId=" + nvl(sourceSelection.getBlockId(), "")
                        + ", blockType=" + sourceSelection.getBlockType()
                        + ", subType=" + nvl(sourceSelection.getSubType(), "")
                        + ", sourceKind=" + nvl(sourceSelection.getSourceKind(), "")
                        + ", sourceText=" + sourceText
        );
        response.setMatchedParagraphIds(List.of(sourceSelection.getParagraphId()));
        response.setStyledParagraphs(List.of());
        response.setOutputPath(outputPath.toString());
        response.setFileId(fileId);
        response.setDownloadUrl("/api/v1/compose/files/" + fileId);

        return response;
    }

    private ComposeRunApplyResponse runTableApply(
            ComposeRunApplyRequest request,
            ComposeSessionService.SessionData session,
            ComposeSelection templateSelection,
            ComposeSelection sourceSelection,
            Path sourceHwpxPath
    ) throws Exception {
        if (templateSelection.getTableId() == null || templateSelection.getTableId().isBlank()) {
            throw new IllegalArgumentException("template tableId is required");
        }
        if (sourceSelection.getTableId() == null || sourceSelection.getTableId().isBlank()) {
            throw new IllegalArgumentException("source tableId is required");
        }

        List<TableNodeDto> templateTables = hwpxTableExtractor.extract(session.getTemplatePath().toFile());
        List<TableNodeDto> sourceTables = hwpxTableExtractor.extract(sourceHwpxPath.toFile());

        TableNodeDto templateTable = findByTableId(templateTables, templateSelection.getTableId());
        TableNodeDto sourceTable = findByTableId(sourceTables, sourceSelection.getTableId());

        if (templateTable.getExtractedStyle() == null) {
            throw new IllegalArgumentException("template extracted table style not found");
        }

        Path outputPath = composeTableStyleApplyService.applyTableStyle(
                sourceHwpxPath,
                sourceSelection.getTableId(),
                templateTable
        );

        String fileId = outputPath.getFileName().toString();

        ComposeRunApplyResponse response = new ComposeRunApplyResponse();
        response.setSessionId(request.getSessionId());
        response.setLabel(request.getLabel());
        response.setTemplateSelectionSummary(
                "template page=" + templateSelection.getPage()
                        + ", label=" + templateSelection.getLabel()
                        + ", blockType=table"
                        + ", tableId=" + templateSelection.getTableId()
                        + ", blockId=" + nvl(templateSelection.getBlockId(), "")
                        + ", subType=" + nvl(templateSelection.getSubType(), "")
                        + ", sourceKind=" + nvl(templateSelection.getSourceKind(), "")
                        + ", rowCount=" + templateTable.getRowCount()
                        + ", colCount=" + templateTable.getColCount()
        );
        response.setSourceSelectionSummary(
                "source page=" + sourceSelection.getPage()
                        + ", label=" + sourceSelection.getLabel()
                        + ", blockType=table"
                        + ", tableId=" + sourceSelection.getTableId()
                        + ", blockId=" + nvl(sourceSelection.getBlockId(), "")
                        + ", subType=" + nvl(sourceSelection.getSubType(), "")
                        + ", sourceKind=" + nvl(sourceSelection.getSourceKind(), "")
                        + ", rowCount=" + sourceTable.getRowCount()
                        + ", colCount=" + sourceTable.getColCount()
        );
        response.setMatchedParagraphIds(List.of());
        response.setStyledParagraphs(List.of());
        response.setOutputPath(outputPath.toString());
        response.setFileId(fileId);
        response.setDownloadUrl("/api/v1/compose/files/" + fileId);

        return response;
    }

    private String resolveIconPrefix(ComposeSelection templateSelection, ParagraphNodeDto templateSample) {
        String fromParagraph = templateSample != null ? iconClassifier.extractPrefix(templateSample.getText()) : null;
        if (fromParagraph != null && !fromParagraph.isBlank()) {
            return fromParagraph;
        }

        String fromOcr = templateSelection != null ? iconClassifier.extractPrefix(templateSelection.getOcrText()) : null;
        if (fromOcr != null && !fromOcr.isBlank()) {
            return fromOcr;
        }

        return null;
    }

    private ComposeSelection toModel(ComposeSelectionResponse dto) {
        ComposeSelection model = new ComposeSelection();
        model.setSessionId(dto.getSessionId());
        model.setSide(dto.getSide());
        model.setPage(dto.getPage());
        model.setX(dto.getX());
        model.setY(dto.getY());
        model.setWidth(dto.getWidth());
        model.setHeight(dto.getHeight());
        model.setLabel(dto.getLabel());
        model.setOcrText(dto.getOcrText());
        model.setParagraphId(dto.getParagraphId());
        model.setBlockType(dto.getBlockType());
        model.setTableId(dto.getTableId());

        model.setBlockId(dto.getBlockId());
        model.setRole(dto.getRole());
        model.setSubType(dto.getSubType());
        model.setSourceKind(dto.getSourceKind());

        return model;
    }

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

    private TableNodeDto findByTableId(List<TableNodeDto> tables, String tableId) {
        if (tableId == null || tableId.isBlank()) {
            throw new IllegalArgumentException("tableId is required");
        }

        for (TableNodeDto t : tables) {
            if (tableId.equals(t.getTableId())) {
                return t;
            }
        }

        throw new IllegalArgumentException("table not found: " + tableId);
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

    private String nvl(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }
}