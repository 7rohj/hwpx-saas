package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.PageLayoutBlockDto;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ComposeLayoutService {

    private static final int PAGE_HEIGHT_LIMIT = 680;
    private static final int PAGE_TOP_PADDING = 36;
    private static final int PAGE_LEFT_PADDING = 56;
    private static final int CONTENT_WIDTH = 420;

    public List<PageLayoutBlockDto> buildLayout(
            List<ParagraphNodeDto> paragraphs,
            List<TableNodeDto> tables,
            String sourceKind
    ) {
        List<PageLayoutBlockDto> blocks = new ArrayList<>();

        if (paragraphs != null) {
            for (int i = 0; i < paragraphs.size(); i++) {
                ParagraphNodeDto p = paragraphs.get(i);

                PageLayoutBlockDto block = new PageLayoutBlockDto();
                block.setBlockType("paragraph");
                block.setBlockId(buildParagraphBlockId(p.getParagraphId(), i));
                block.setParagraphId(p.getParagraphId());
                block.setTableId(null);
                block.setRole(null);
                block.setSubType("paragraph");
                block.setSourceKind(normalizeSourceKind(sourceKind));

                block.setOrderIndex(extractNumericOrder(p.getParagraphId(), i));
                block.setEstimatedFontSize(p.getFontSize() != null ? p.getFontSize() : 12);
                block.setHeadingLike(isHeadingLike(p));
                block.setTextLength(p.getText() == null ? 0 : p.getText().length());

                blocks.add(block);
            }
        }

        if (tables != null) {
            for (int i = 0; i < tables.size(); i++) {
                TableNodeDto t = tables.get(i);

                PageLayoutBlockDto block = new PageLayoutBlockDto();
                block.setBlockType("table");
                block.setBlockId(buildTableBlockId(t.getTableId(), i));
                block.setParagraphId(null);
                block.setTableId(t.getTableId());
                block.setRole(null);
                block.setSubType(resolveTableSubType(t));
                block.setSourceKind(normalizeSourceKind(sourceKind));

                block.setOrderIndex(extractNumericOrder(t.getTableId(), 100000 + i));
                block.setEstimatedRows(t.getRowCount() != null ? t.getRowCount() : 1);
                block.setEstimatedCols(t.getColCount() != null ? t.getColCount() : 1);

                blocks.add(block);
            }
        }

        blocks.sort(Comparator.comparing(
                b -> b.getOrderIndex() == null ? Integer.MAX_VALUE : b.getOrderIndex()
        ));

        int cursorY = PAGE_TOP_PADDING;

        for (PageLayoutBlockDto block : blocks) {
            int height = estimateBlockHeight(block);
            int spacingBefore = estimateSpacingBefore(block);
            int spacingAfter = estimateSpacingAfter(block);

            cursorY += spacingBefore;

            int page = (cursorY / PAGE_HEIGHT_LIMIT) + 1;
            int normalizedTop = cursorY % PAGE_HEIGHT_LIMIT;

            if (normalizedTop + height > PAGE_HEIGHT_LIMIT - PAGE_TOP_PADDING) {
                cursorY = page * PAGE_HEIGHT_LIMIT + PAGE_TOP_PADDING;
                page = (cursorY / PAGE_HEIGHT_LIMIT) + 1;
                normalizedTop = cursorY % PAGE_HEIGHT_LIMIT;
            }

            block.setPage(page);
            block.setX((double) PAGE_LEFT_PADDING);
            block.setY((double) normalizedTop);
            block.setWidth((double) estimateBlockWidth(block));
            block.setHeight((double) height);

            cursorY += height + spacingAfter;
        }

        return blocks;
    }

    public void applyLayoutToParagraphs(
            List<ParagraphNodeDto> paragraphs,
            List<PageLayoutBlockDto> layoutBlocks
    ) {
        if (paragraphs == null || layoutBlocks == null) return;

        for (ParagraphNodeDto p : paragraphs) {
            PageLayoutBlockDto matched = findParagraphBlock(layoutBlocks, p.getParagraphId());
            if (matched == null) continue;

            p.setPage(matched.getPage());
            p.setX(matched.getX());
            p.setY(matched.getY());
            p.setWidth(matched.getWidth());
            p.setHeight(matched.getHeight());
        }
    }

    public void applyLayoutToTables(
            List<TableNodeDto> tables,
            List<PageLayoutBlockDto> layoutBlocks
    ) {
        if (tables == null || layoutBlocks == null) return;

        for (TableNodeDto t : tables) {
            PageLayoutBlockDto matched = findTableBlock(layoutBlocks, t.getTableId());
            if (matched == null) continue;

            t.setPage(matched.getPage());
            t.setX(matched.getX());
            t.setY(matched.getY());
            t.setWidth(matched.getWidth());
            t.setHeight(matched.getHeight());
        }
    }

    private PageLayoutBlockDto findParagraphBlock(List<PageLayoutBlockDto> blocks, String paragraphId) {
        for (PageLayoutBlockDto b : blocks) {
            if ("paragraph".equals(b.getBlockType()) && equalsSafe(paragraphId, b.getParagraphId())) {
                return b;
            }
        }
        return null;
    }

    private PageLayoutBlockDto findTableBlock(List<PageLayoutBlockDto> blocks, String tableId) {
        for (PageLayoutBlockDto b : blocks) {
            if ("table".equals(b.getBlockType()) && equalsSafe(tableId, b.getTableId())) {
                return b;
            }
        }
        return null;
    }

    private int estimateBlockWidth(PageLayoutBlockDto block) {
        if (block == null) return CONTENT_WIDTH;

        if ("table".equals(block.getBlockType())) {
            int cols = block.getEstimatedCols() != null ? block.getEstimatedCols() : 1;
            return Math.max(260, Math.min(520, cols * 92));
        }

        return CONTENT_WIDTH;
    }

    private int estimateBlockHeight(PageLayoutBlockDto block) {
        if (block == null) return 34;

        if ("table".equals(block.getBlockType())) {
            int rows = block.getEstimatedRows() != null ? block.getEstimatedRows() : 1;
            return Math.max(72, rows * 42 + 12);
        }

        int fontSize = block.getEstimatedFontSize() != null ? block.getEstimatedFontSize() : 12;
        int textLength = block.getTextLength() != null ? block.getTextLength() : 0;

        int estimatedLines = Math.max(1, (int) Math.ceil(textLength / 20.0));
        int lineHeight = (int) Math.round(fontSize * 1.65);

        return Math.max(28, estimatedLines * lineHeight);
    }

    private int estimateSpacingBefore(PageLayoutBlockDto block) {
        if (block == null) return 6;

        if ("table".equals(block.getBlockType())) {
            return 16;
        }

        if (Boolean.TRUE.equals(block.getHeadingLike())) {
            return 18;
        }

        return 6;
    }

    private int estimateSpacingAfter(PageLayoutBlockDto block) {
        if (block == null) return 4;

        if ("table".equals(block.getBlockType())) {
            return 18;
        }

        if (Boolean.TRUE.equals(block.getHeadingLike())) {
            return 10;
        }

        return 4;
    }

    private boolean isHeadingLike(ParagraphNodeDto p) {
        if (p == null) return false;

        int fontSize = p.getFontSize() != null ? p.getFontSize() : 12;
        boolean bold = Boolean.TRUE.equals(p.getBold());
        String text = p.getText() == null ? "" : p.getText().trim();

        if (fontSize >= 14) return true;
        if (bold && text.length() <= 30) return true;
        return text.matches("^\\d+(\\.\\d+)*\\s+.*");
    }

    private String buildParagraphBlockId(String paragraphId, int index) {
        if (paragraphId != null && !paragraphId.isBlank()) {
            return "pblock_" + paragraphId;
        }
        return "pblock_idx_" + index;
    }

    private String buildTableBlockId(String tableId, int index) {
        if (tableId != null && !tableId.isBlank()) {
            return "tblock_" + tableId;
        }
        return "tblock_idx_" + index;
    }

    private String resolveTableSubType(TableNodeDto table) {
        if (table == null || table.getTableKind() == null || table.getTableKind().isBlank()) {
            return "table";
        }

        String kind = table.getTableKind().trim().toLowerCase();
        if ("oneline_box".equals(kind) || "onebox_line".equals(kind)) {
            return "onebox_line";
        }
        return "table";
    }

    private String normalizeSourceKind(String sourceKind) {
        if (sourceKind == null || sourceKind.isBlank()) {
            return "source";
        }

        String v = sourceKind.trim().toLowerCase();
        if ("template".equals(v)) {
            return "template";
        }
        return "source";
    }

    private int extractNumericOrder(String rawId, int fallback) {
        if (rawId == null || rawId.isBlank()) {
            return fallback;
        }

        String digits = rawId.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return fallback;
        }

        try {
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return fallback;
        }
    }

    private boolean equalsSafe(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }
}