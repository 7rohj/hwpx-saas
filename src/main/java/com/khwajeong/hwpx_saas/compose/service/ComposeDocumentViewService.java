package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.PageLayoutBlockDto;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class ComposeDocumentViewService {

    private static final double PREVIEW_VERTICAL_COMPACT_RATIO = 0.5;
    private static final double PREVIEW_PAGE_HEIGHT_LIMIT = 680.0;

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

        // 6. preview용 위치 보정
        //    - 문단/표의 전체 세로 배치를 1/2로 압축
        //    - 결과적으로 표와 텍스트 사이 간격도 1/2로 줄어듦
        //    - page도 다시 계산하여 실제 preview 페이지 수를 줄임
        compactPreviewLayout(paragraphs, tables);

        // 7. 결과 반환
        DocumentViewResult result = new DocumentViewResult();
        result.setParagraphs(paragraphs);
        result.setTables(tables);

        return result;
    }

    private void compactPreviewLayout(
            List<ParagraphNodeDto> paragraphs,
            List<TableNodeDto> tables
    ) {
        if (paragraphs != null) {
            for (ParagraphNodeDto p : paragraphs) {
                double originalGlobalY = toGlobalY(p.getPage(), p.getY());
                double compactedGlobalY = originalGlobalY * PREVIEW_VERTICAL_COMPACT_RATIO;

                int newPage = (int) Math.floor(compactedGlobalY / PREVIEW_PAGE_HEIGHT_LIMIT) + 1;
                double newY = compactedGlobalY % PREVIEW_PAGE_HEIGHT_LIMIT;

                p.setPage(newPage);
                p.setY(newY);

                // 문단 높이는 너무 과하게 줄이면 클릭 영역이 불편해질 수 있어서
                // 위치만 압축하고 높이는 유지
            }
        }

        if (tables != null) {
            for (TableNodeDto t : tables) {
                double originalGlobalY = toGlobalY(t.getPage(), t.getY());
                double compactedGlobalY = originalGlobalY * PREVIEW_VERTICAL_COMPACT_RATIO;

                int newPage = (int) Math.floor(compactedGlobalY / PREVIEW_PAGE_HEIGHT_LIMIT) + 1;
                double newY = compactedGlobalY % PREVIEW_PAGE_HEIGHT_LIMIT;

                t.setPage(newPage);
                t.setY(newY);

                // 표 전체 블록 높이도 같이 줄여야
                // preview 상에서 표와 다음 텍스트 사이 간격이 함께 줄어듦
                if (t.getHeight() != null) {
                    t.setHeight(t.getHeight() * PREVIEW_VERTICAL_COMPACT_RATIO);
                }
            }
        }
    }

    private double toGlobalY(Integer page, Double y) {
        int safePage = (page == null || page < 1) ? 1 : page;
        double safeY = y == null ? 0.0 : y;
        return ((safePage - 1) * PREVIEW_PAGE_HEIGHT_LIMIT) + safeY;
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