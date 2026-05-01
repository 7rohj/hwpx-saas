package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.*;
import com.khwajeong.hwpx_saas.compose.model.ComposeSelection;
import org.springframework.stereotype.Service;
import com.khwajeong.hwpx_saas.billing.service.CreditService;
import com.khwajeong.hwpx_saas.billing.service.DailyUsageLimitService;

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
    private final CreditService creditService;
    private final ComposeOutputStorageService composeOutputStorageService;
    private final DailyUsageLimitService dailyUsageLimitService;

    public ComposeApplyRunnerService(
            ComposeSelectionService composeSelectionService,
            ComposeService composeService,
            ComposeSessionService composeSessionService,
            HwpxStyleExtractor hwpxStyleExtractor,
            ComposeTextStyleApplyService composeTextStyleApplyService,
            HwpxTableExtractor hwpxTableExtractor,
            ComposeTableStyleApplyService composeTableStyleApplyService,
            IconClassifier iconClassifier,
            CreditService creditService,
            ComposeOutputStorageService composeOutputStorageService,
            DailyUsageLimitService dailyUsageLimitService
    ) {
        this.composeSelectionService = composeSelectionService;
        this.composeService = composeService;
        this.composeSessionService = composeSessionService;
        this.hwpxStyleExtractor = hwpxStyleExtractor;
        this.composeTextStyleApplyService = composeTextStyleApplyService;
        this.hwpxTableExtractor = hwpxTableExtractor;
        this.composeTableStyleApplyService = composeTableStyleApplyService;
        this.iconClassifier = iconClassifier;
        this.creditService = creditService;
        this.composeOutputStorageService = composeOutputStorageService;
        this.dailyUsageLimitService = dailyUsageLimitService;
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

            List<ComposeSelection> sourceSelections =
                    composeSelectionService.findAllBySessionIdAndSideAndLabel(
                            request.getSessionId(), "source", request.getLabel()
                    );

            if (templateSelection == null) {
                throw new IllegalArgumentException("template selection not found for label: " + request.getLabel());
            }
            if (sourceSelections == null || sourceSelections.isEmpty()) {
                throw new IllegalArgumentException("source selections not found for label: " + request.getLabel());
            }

            Path workingSourcePath = session.getSourcePath();
            List<String> appliedTargets = new ArrayList<>();
            List<String> allMatchedParagraphIds = new ArrayList<>();
            List<ParagraphNodeDto> allStyledParagraphs = new ArrayList<>();

            for (ComposeSelection sourceSelection : sourceSelections) {
                ComposeRunApplyResponse singleResponse = runSingleApply(
                        request,
                        session,
                        templateSelection,
                        sourceSelection,
                        workingSourcePath
                );

                workingSourcePath = Path.of(singleResponse.getOutputPath());

                if (singleResponse.getMatchedParagraphIds() != null) {
                    allMatchedParagraphIds.addAll(singleResponse.getMatchedParagraphIds());
                }
                if (singleResponse.getStyledParagraphs() != null) {
                    allStyledParagraphs.addAll(singleResponse.getStyledParagraphs());
                }

                appliedTargets.add(buildTargetSummary(sourceSelection));
            }

            String fileId = workingSourcePath.getFileName().toString();

            ComposeRunApplyResponse finalResponse = new ComposeRunApplyResponse();
            finalResponse.setSessionId(request.getSessionId());
            finalResponse.setLabel(request.getLabel());
            finalResponse.setTemplateSelectionSummary(
                    "template label=" + templateSelection.getLabel()
                            + ", blockType=" + nvl(templateSelection.getBlockType(), "")
                            + ", paragraphId=" + nvl(templateSelection.getParagraphId(), "")
                            + ", tableId=" + nvl(templateSelection.getTableId(), "")
            );
            finalResponse.setSourceSelectionSummary(
                    "source applied targets = " + appliedTargets
            );
            finalResponse.setMatchedParagraphIds(dedupPreserveOrder(allMatchedParagraphIds));
            finalResponse.setStyledParagraphs(dedupParagraphsPreserveOrder(allStyledParagraphs));
            ComposeOutputStorageService.StoredComposeFile stored =
                    composeOutputStorageService.uploadOutput(
                            workingSourcePath,
                            "anonymous",
                            request.getSessionId()
                    );

            finalResponse.setOutputPath(stored.objectKey());
            finalResponse.setFileId(stored.fileId());
            finalResponse.setDownloadUrl(stored.downloadUrl());

            return finalResponse;

        } catch (Exception e) {
            System.out.println("=== [COMPOSE APPLY ERROR] ===");
            e.printStackTrace();
            throw new RuntimeException("compose apply failed: " + e.getMessage(), e);
        }
    }

    public ComposeRunApplyResponse runAll(ComposeRunApplyAllRequest request) {
        int deductedCredits = 0;
        String userEmail = "test@test.com"; // TODO: 로그인 붙이면 인증 사용자 email로 교체

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

            // 추가: pageCount 계산
            int pageCount = calculatePageCount(session);

            dailyUsageLimitService.checkAndIncrease(userEmail, "COMPOSE_APPLY", 20);

            // 추가: 필요 코인 계산 후 차감
            deductedCredits = creditService.calculateRequiredCredits(pageCount);
            creditService.checkAndDeduct(userEmail, pageCount);

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

            Map<String, List<ComposeSelectionResponse>> sourceMap = allSelections.stream()
                    .filter(s -> "source".equalsIgnoreCase(s.getSide()))
                    .collect(Collectors.groupingBy(
                            ComposeSelectionResponse::getLabel,
                            LinkedHashMap::new,
                            Collectors.toList()
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
                List<ComposeSelectionResponse> sourceList = sourceMap.get(label);

                if (templateSel == null || sourceList == null || sourceList.isEmpty()) {
                    continue;
                }

                for (ComposeSelectionResponse sourceSel : sourceList) {
                    ComposeRunApplyRequest singleRequest = new ComposeRunApplyRequest();
                    singleRequest.setSessionId(request.getSessionId());
                    singleRequest.setLabel(label);
                    singleRequest.setFontPolicy(request.getFontPolicy());

                    ComposeSelection templateModel = toModel(templateSel);
                    ComposeSelection sourceModel = toModel(sourceSel);

                    ComposeRunApplyResponse singleResponse = runSingleApply(
                            singleRequest,
                            session,
                            templateModel,
                            sourceModel,
                            workingSourcePath
                    );

                    workingSourcePath = Path.of(singleResponse.getOutputPath());
                    appliedLabels.add(label);
                    appliedTargets.add(buildTargetSummary(sourceModel));
                }
            }

            if (appliedLabels.isEmpty()) {
                throw new IllegalArgumentException("no applicable label pairs found");
            }

            ComposeRunApplyResponse finalResponse = new ComposeRunApplyResponse();
            finalResponse.setSessionId(request.getSessionId());
            finalResponse.setLabel(String.join(",", appliedLabels));
            finalResponse.setTemplateSelectionSummary(
                    "template applied labels = " + appliedLabels
                            + ", deductedCredits=" + deductedCredits
                            + ", userEmail=" + userEmail
            );
            finalResponse.setSourceSelectionSummary("source applied targets = " + appliedTargets);
            finalResponse.setMatchedParagraphIds(List.of());
            finalResponse.setStyledParagraphs(List.of());
            ComposeOutputStorageService.StoredComposeFile stored =
                    composeOutputStorageService.uploadOutput(
                            workingSourcePath,
                            userEmail,
                            request.getSessionId()
                    );

            finalResponse.setOutputPath(stored.objectKey());
            finalResponse.setFileId(stored.fileId());
            finalResponse.setDownloadUrl(stored.downloadUrl());

            return finalResponse;

        } catch (Exception e) {
            // 추가: 실패 시 차감된 코인 환불
            if (deductedCredits > 0) {
                try {
                    creditService.refund(userEmail, deductedCredits);
                } catch (Exception refundError) {
                    System.out.println("=== [CREDIT REFUND ERROR] ===");
                    refundError.printStackTrace();
                }
            }

            System.out.println("=== [COMPOSE APPLY-ALL ERROR] ===");
            e.printStackTrace();

            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            throw new RuntimeException("compose apply-all failed: " + e.getMessage(), e);
        }
    }

    private ComposeRunApplyResponse runSingleApply(
            ComposeRunApplyRequest request,
            ComposeSessionService.SessionData session,
            ComposeSelection templateSelection,
            ComposeSelection sourceSelection,
            Path sourceHwpxPath
    ) throws Exception {

        if ("table".equalsIgnoreCase(templateSelection.getBlockType())
                && "oneline_box".equalsIgnoreCase(templateSelection.getLabel())) {
            return runOnelineBoxApply(request, session, templateSelection, sourceSelection, sourceHwpxPath);
        }

        if ("table".equalsIgnoreCase(templateSelection.getBlockType())
                || "table".equalsIgnoreCase(sourceSelection.getBlockType())) {
            return runTableApply(request, session, templateSelection, sourceSelection, sourceHwpxPath);
        }

        return runParagraphApply(request, session, templateSelection, sourceSelection, sourceHwpxPath);
    }

    private String buildTargetSummary(ComposeSelection sourceSelection) {
        if (sourceSelection == null) {
            return "";
        }

        if ("table".equalsIgnoreCase(sourceSelection.getBlockType())) {
            return "table:" + nvl(sourceSelection.getBlockId(), sourceSelection.getTableId());
        }

        if ("oneline_box".equalsIgnoreCase(sourceSelection.getLabel())) {
            return "oneline_box:" + nvl(sourceSelection.getBlockId(), sourceSelection.getParagraphId());
        }

        return "paragraph:" + nvl(sourceSelection.getBlockId(), sourceSelection.getParagraphId());
    }

    private List<String> dedupPreserveOrder(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private List<ParagraphNodeDto> dedupParagraphsPreserveOrder(List<ParagraphNodeDto> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        Map<String, ParagraphNodeDto> ordered = new LinkedHashMap<>();
        for (ParagraphNodeDto dto : values) {
            if (dto == null) {
                continue;
            }

            String key = dto.getParagraphId();
            if (key == null || key.isBlank()) {
                key = UUID.randomUUID().toString();
            }

            ordered.putIfAbsent(key, dto);
        }

        return new ArrayList<>(ordered.values());
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

    private int calculatePageCount(ComposeSessionService.SessionData session) {
        try {
            List<ParagraphNodeDto> paragraphs =
                    composeService.extractFromHwpx(session.getSourcePath().toFile());

            int paragraphMaxPage = paragraphs == null
                    ? 1
                    : paragraphs.stream()
                            .map(ParagraphNodeDto::getPage)
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(1);

            List<TableNodeDto> tables =
                    hwpxTableExtractor.extract(session.getSourcePath().toFile());

            int tableMaxPage = tables == null
                    ? 1
                    : tables.stream()
                            .map(TableNodeDto::getPage)
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(1);

            return Math.max(1, Math.max(paragraphMaxPage, tableMaxPage));
        } catch (Exception e) {
            return 1;
        }
    }
}