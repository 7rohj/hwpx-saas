package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.PageLayoutBlockDto;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ComposeDocumentViewService {

    private final ComposeService composeService;
    private final HwpxTableExtractor hwpxTableExtractor;
    private final ComposeLayoutService composeLayoutService;

    public ComposeDocumentViewService(
            ComposeService composeService,
            HwpxTableExtractor hwpxTableExtractor,
            ComposeLayoutService composeLayoutService
    ) {
        this.composeService = composeService;
        this.hwpxTableExtractor = hwpxTableExtractor;
        this.composeLayoutService = composeLayoutService;
    }

    public DocumentViewResult build(File hwpxFile) {

        if (hwpxFile == null || !hwpxFile.exists()) {
            throw new IllegalArgumentException("hwpx file not found");
        }

        // 1. 문단 추출
        List<ParagraphNodeDto> paragraphs =
                composeService.extractFromHwpx(hwpxFile);

        // 2. 표 추출
        List<TableNodeDto> tables =
                hwpxTableExtractor.extract(hwpxFile);

        // 3. 공통 레이아웃 계산
        List<PageLayoutBlockDto> layout =
                composeLayoutService.buildLayout(paragraphs, tables);

        // 4. paragraph 좌표 반영
        composeLayoutService.applyLayoutToParagraphs(paragraphs, layout);

        // 5. table 좌표 반영
        composeLayoutService.applyLayoutToTables(tables, layout);

        // 6. 결과 반환
        DocumentViewResult result = new DocumentViewResult();
        result.setParagraphs(paragraphs);
        result.setTables(tables);

        return result;
    }

    // =========================
    // 내부 DTO (응답용)
    // =========================
    public static class DocumentViewResult {
        private List<ParagraphNodeDto> paragraphs;
        private List<TableNodeDto> tables;

        public List<ParagraphNodeDto> getParagraphs() {
            return paragraphs;
        }

        public void setParagraphs(List<ParagraphNodeDto> paragraphs) {
            this.paragraphs = paragraphs;
        }

        public List<TableNodeDto> getTables() {
            return tables;
        }

        public void setTables(List<TableNodeDto> tables) {
            this.tables = tables;
        }
    }
}