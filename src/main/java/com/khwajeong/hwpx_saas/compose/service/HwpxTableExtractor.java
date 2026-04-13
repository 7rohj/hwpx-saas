package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import com.khwajeong.hwpx_saas.compose.dto.TableCellDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
import com.khwajeong.hwpx_saas.compose.model.ExtractedTableStyle;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class HwpxTableExtractor {

    private static final int PREVIEW_MAX_TABLE_WIDTH = 520;
    private static final int PREVIEW_MIN_TABLE_WIDTH = 320;
    private static final int DEFAULT_COL_WIDTH = 56;
    private static final int DEFAULT_ROW_HEIGHT = 44;
    private static final int PREVIEW_PAGE_HEIGHT = 680;
    private static final double LARGE_FORM_MIN_HEIGHT_RATIO = 0.85;

    private final HwpxStyleExtractor hwpxStyleExtractor;

    public HwpxTableExtractor(HwpxStyleExtractor hwpxStyleExtractor) {
        this.hwpxStyleExtractor = hwpxStyleExtractor;
    }

    private enum TableType {
        HORIZONTAL_TABLE,
        VERTICAL_FORM,
        MIXED_FORM
    }

    public List<TableNodeDto> extract(File hwpxFile) {
        if (hwpxFile == null || !hwpxFile.exists()) {
            throw new IllegalArgumentException("hwpx file not found");
        }

        List<TableNodeDto> result = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(hwpxFile)) {
            List<ZipEntry> sectionEntries = findSectionEntries(zipFile);
            Document headerDoc = loadHeaderDoc(zipFile);

            int tableIndex = 0;
            int virtualTopCursor = 80;

            for (ZipEntry sectionEntry : sectionEntries) {
                byte[] sectionBytes;
                try (InputStream in = zipFile.getInputStream(sectionEntry)) {
                    sectionBytes = in.readAllBytes();
                }

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);

                Document doc = factory.newDocumentBuilder()
                        .parse(new InputSource(new ByteArrayInputStream(sectionBytes)));

                NodeList tblList = doc.getElementsByTagName("hp:tbl");

                for (int i = 0; i < tblList.getLength(); i++) {
                    Node node = tblList.item(i);
                    if (!(node instanceof Element tblEl)) {
                        continue;
                    }

                    TableNodeDto dto = new TableNodeDto();
                    dto.setTableId("t" + tableIndex);

                    List<Element> rowElements = getDirectChildElements(tblEl, "hp:tr", "tr");
                    List<TableCellDto> cells = new ArrayList<>();

                    Map<Integer, Map<Integer, Boolean>> occupied = new HashMap<>();
                    int maxColCount = 0;

                    for (int rowIndex = 0; rowIndex < rowElements.size(); rowIndex++) {
                        Element rowEl = rowElements.get(rowIndex);
                        List<Element> cellElements = getDirectChildElements(rowEl, "hp:tc", "tc");

                        int currentCol = 0;

                        for (Element cellEl : cellElements) {
                            Integer explicitRowAddr = readIntAttr(
                                    cellEl,
                                    "rowAddr", "rowaddr", "row", "rowIndex"
                            );
                            Integer explicitColAddr = readIntAttr(
                                    cellEl,
                                    "colAddr", "coladdr", "col", "colIndex"
                            );

                            int actualRow = explicitRowAddr != null ? explicitRowAddr : rowIndex;
                            currentCol = explicitColAddr != null
                                    ? explicitColAddr
                                    : findNextAvailableCol(occupied, actualRow, currentCol);

                            int rowSpan = readSpan(
                                    cellEl,
                                    "rowSpan", "rowspan", "rowSpanCount",
                                    "vertSpan", "vSpan", "rowMerge"
                            );
                            int colSpan = readSpan(
                                    cellEl,
                                    "colSpan", "colspan", "colSpanCount",
                                    "horzSpan", "hSpan", "colMerge"
                            );

                            String cellText = extractCellText(cellEl);
                            StyleProfileDto cellStyle = extractCellRepresentativeStyle(hwpxFile, cellEl);

                            TableCellDto cell = new TableCellDto();
                            cell.setRowIndex(actualRow);
                            cell.setColIndex(currentCol);
                            cell.setRowSpan(rowSpan);
                            cell.setColSpan(colSpan);
                            cell.setText(cellText);

                            String borderFillId = readAnyAttr(cellEl, "borderFillIDRef", "borderFillIdRef");
                            String backgroundColor = parseBackgroundColorFromHeader(
                                    headerDoc, borderFillId, null
                            );
                            cell.setBackgroundColor(backgroundColor);

                            if (cellStyle != null) {
                                cell.setFontFamily(cellStyle.getFontFamily());
                                cell.setFontSize(cellStyle.getFontSize());
                                cell.setBold(cellStyle.getBold());
                                cell.setItalic(cellStyle.getItalic());
                                cell.setUnderline(cellStyle.getUnderline());
                                cell.setFontColor(cellStyle.getColor());
                                cell.setAlign(cellStyle.getAlign());
                            }

                            cells.add(cell);

                            markOccupied(occupied, actualRow, currentCol, rowSpan, colSpan);
                            currentCol += colSpan;
                            maxColCount = Math.max(maxColCount, currentCol);
                        }
                    }

                    int rowCount = calculateRowCount(cells, rowElements.size());
                    int colCount = Math.max(1, maxColCount);

                    assignSemanticRoles(cells, rowCount, colCount);

                    dto.setRowCount(rowCount);
                    dto.setColCount(colCount);
                    dto.setCells(cells);
                    dto.setCellTexts(buildCellTextsGrid(cells, rowCount, colCount));

                    ExtractedTableStyle extractedStyle = extractTableStyle(hwpxFile, headerDoc, tblEl);
                    dto.setExtractedStyle(extractedStyle);

                    if (extractedStyle != null && extractedStyle.getHeaderTextStyle() != null) {
                        dto.setFontFamily(extractedStyle.getHeaderTextStyle().getFontFamily());
                        dto.setFontSize(extractedStyle.getHeaderTextStyle().getFontSize());
                        dto.setBold(extractedStyle.getHeaderTextStyle().getBold());
                        dto.setItalic(extractedStyle.getHeaderTextStyle().getItalic());
                        dto.setFontColor(extractedStyle.getHeaderTextStyle().getFontColor());
                        dto.setBackgroundColor(extractedStyle.getHeaderBackgroundColor());
                    }

                    int baseX = 56;
                    int baseY = virtualTopCursor;

                    int tableWidth = Math.max(
                            PREVIEW_MIN_TABLE_WIDTH,
                            Math.min(PREVIEW_MAX_TABLE_WIDTH, colCount * DEFAULT_COL_WIDTH)
                    );

                    int tableHeight = estimatePreviewTableHeight(cells, rowCount, colCount);

                    int pageHeightLimit = PREVIEW_PAGE_HEIGHT;
                    int page = (baseY / pageHeightLimit) + 1;
                    int normalizedTop = baseY % pageHeightLimit;

                    dto.setPage(page);
                    dto.setX((double) baseX);
                    dto.setY((double) normalizedTop);
                    dto.setWidth((double) tableWidth);
                    dto.setHeight((double) tableHeight);

                    populateCellRelativeGeometry(dto);

                    virtualTopCursor += tableHeight + 24;

                    result.add(dto);
                    tableIndex++;
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract tables from hwpx", e);
        }

        return result;
    }

    private int calculateRowCount(List<TableCellDto> cells, int fallback) {
        int max = fallback;

        for (TableCellDto cell : cells) {
            int endRow = cell.getRowIndex() + Math.max(1, cell.getRowSpan());
            max = Math.max(max, endRow);
        }

        return Math.max(1, max);
    }

    /**
     * 1) 표 타입 먼저 판별
     * 2) 타입별 role 분류
     */
    private void assignSemanticRoles(List<TableCellDto> cells, int rowCount, int colCount) {
        TableType tableType = detectTableType(cells, rowCount, colCount);

        switch (tableType) {
            case HORIZONTAL_TABLE -> assignHorizontalRoles(cells);
            case VERTICAL_FORM -> assignVerticalRoles(cells, rowCount);
            case MIXED_FORM -> assignMixedRoles(cells, rowCount);
        }
    }

    /**
     * 표 타입 판별 점수 방식
     */
    private TableType detectTableType(List<TableCellDto> cells, int rowCount, int colCount) {
        int horizontalScore = 0;
        int verticalScore = 0;
        int mixedScore = 0;

        if (hasAnyRowSpanGreaterThanOne(cells)) {
            mixedScore += 5;
        }

        if (isHorizontalHeaderLike(cells)) {
            horizontalScore += 4;
        }

        if (isVerticalLabelValueLike(cells, rowCount)) {
            verticalScore += 4;
        }

        if (hasRepeatedRowValuePairs(cells, rowCount)) {
            mixedScore += 4;
        }

        if (hasUniformBodyRows(cells, rowCount, colCount)) {
            horizontalScore += 3;
        }

        if (isMostlyLeftLabelRightValue(cells, rowCount)) {
            verticalScore += 3;
        }

        if (mixedScore >= horizontalScore && mixedScore >= verticalScore) {
            return TableType.MIXED_FORM;
        }
        if (horizontalScore >= verticalScore) {
            return TableType.HORIZONTAL_TABLE;
        }
        return TableType.VERTICAL_FORM;
    }

    /**
     * 가로형 데이터 표:
     * - 첫 행 = HEADER_COLUMN
     * - 나머지 = VALUE
     */
    private void assignHorizontalRoles(List<TableCellDto> cells) {
        for (TableCellDto cell : cells) {
            String text = normalizeCellText(cell.getText());

            if (text.isBlank()) {
                cell.setRole("VALUE");
                cell.setHeader(false);
                continue;
            }

            if (cell.getRowIndex() == 0) {
                cell.setRole("HEADER_COLUMN");
                cell.setHeader(true);
            } else {
                cell.setRole("VALUE");
                cell.setHeader(false);
            }
        }
    }

    /**
     * 세로형 폼:
     * - 각 row 의 첫 번째 의미 있는 셀 = HEADER_ROW
     * - 나머지 = VALUE
     */
    private void assignVerticalRoles(List<TableCellDto> cells, int rowCount) {
        for (TableCellDto cell : cells) {
            cell.setRole(null);
            cell.setHeader(false);
        }

        Map<Integer, List<TableCellDto>> rows = groupCellsByRow(cells);

        for (int r = 0; r < rowCount; r++) {
            List<TableCellDto> row = rows.getOrDefault(r, new ArrayList<>());
            row.sort(Comparator.comparingInt(TableCellDto::getColIndex));

            boolean firstMeaningfulCellAssigned = false;

            for (TableCellDto cell : row) {
                String text = normalizeCellText(cell.getText());

                if (text.isBlank()) {
                    cell.setRole("VALUE");
                    cell.setHeader(false);
                    continue;
                }

                if (!firstMeaningfulCellAssigned) {
                    cell.setRole("HEADER_ROW");
                    cell.setHeader(true);
                    firstMeaningfulCellAssigned = true;
                } else {
                    cell.setRole("VALUE");
                    cell.setHeader(false);
                }
            }
        }

        for (TableCellDto cell : cells) {
            if (cell.getRole() == null) {
                cell.setRole("VALUE");
                cell.setHeader(false);
            }
        }
    }

    /**
     * 혼합형 표:
     *
     * 핵심 규칙
     * 1. rowSpan > 1 => HEADER_GROUP
     * 2. group 하위 셀들은 HEADER_ROW
     * 3. HEADER_ROW 에 종속되는 셀들은 VALUE
     * 4. VALUE 는 빈칸일 수도 있고, 텍스트가 있을 수도 있다
     *
     * 구현 포인트
     * - 세로 병합 그룹을 먼저 확정
     * - group 영향권 행에서 "row 스타일 시그니처"를 찾는다
     * - row 스타일:
     *   회색 배경, bold, 짧은 텍스트 같은 특성을 점수화
     * - 반복형 행:
     *   같은 row 스타일이 2회 이상 반복되면
     *   그 셀들은 HEADER_ROW, 나머지는 VALUE
     * - 단일형 행:
     *   첫 번째 row 스타일 셀만 HEADER_ROW, 나머지는 VALUE
     */
    private void assignMixedRoles(List<TableCellDto> cells, int rowCount) {
        for (TableCellDto cell : cells) {
            cell.setRole(null);
            cell.setHeader(false);
        }

        // 1) 세로 병합 셀은 GROUP
        for (TableCellDto cell : cells) {
            String text = normalizeCellText(cell.getText());

            if (text.isBlank()) {
                cell.setRole("VALUE");
                cell.setHeader(false);
                continue;
            }

            if (cell.getRowSpan() > 1) {
                cell.setRole("HEADER_GROUP");
                cell.setHeader(true);
            }
        }

        Map<Integer, List<TableCellDto>> rows = groupCellsByRow(cells);

        for (int r = 0; r < rowCount; r++) {
            List<TableCellDto> row = rows.getOrDefault(r, new ArrayList<>());
            row.sort(Comparator.comparingInt(TableCellDto::getColIndex));

            if (row.isEmpty()) {
                continue;
            }

            boolean rowCoveredByGroup = isRowCoveredByAnyGroup(r, cells);
            boolean rowHasDirectGroup = row.stream().anyMatch(c -> "HEADER_GROUP".equals(c.getRole()));

            if (!rowCoveredByGroup && !rowHasDirectGroup) {
                // 혼합형이라도 일반 행이 있을 수 있음:
                // 첫 의미 있는 셀은 row, 나머지는 value
                assignSimpleRowValuePattern(row);
                continue;
            }

            // group 자신은 제외하고 의미 있는 셀만 추출
            List<TableCellDto> nonGroupCells = new ArrayList<>();
            for (TableCellDto cell : row) {
                if ("HEADER_GROUP".equals(cell.getRole())) {
                    continue;
                }
                nonGroupCells.add(cell);
            }

            if (nonGroupCells.isEmpty()) {
                continue;
            }

            // 이 행에서 row 스타일 후보 signature 찾기
            String dominantRowStyle = findDominantRowStyleSignature(nonGroupCells);

            if (dominantRowStyle != null && countCellsWithSignature(nonGroupCells, dominantRowStyle) >= 2) {
                // 반복형: 같은 row 스타일이 2개 이상 반복됨
                // 예: 1순위 / 3인실 / 2순위 / 5인실 ...
                for (TableCellDto cell : nonGroupCells) {
                    if (normalizeCellText(cell.getText()).isBlank()) {
                        if (cell.getRole() == null) {
                            cell.setRole("VALUE");
                            cell.setHeader(false);
                        }
                        continue;
                    }

                    if (dominantRowStyle.equals(buildStyleSignature(cell))) {
                        cell.setRole("HEADER_ROW");
                        cell.setHeader(true);
                    } else {
                        cell.setRole("VALUE");
                        cell.setHeader(false);
                    }
                }
            } else {
                // 단일형: 첫 row 스타일 셀 하나만 row, 나머지 value
                // 예: 입주형태(예정) | □ | 본사 이전 | □ | 부설연구소 이전 등
                // 예: 상주인원(현재) | 대표자 포함 | 명
                TableCellDto firstRowLike = findFirstRowLikeCell(nonGroupCells);

                for (TableCellDto cell : nonGroupCells) {
                    if (cell == firstRowLike) {
                        cell.setRole("HEADER_ROW");
                        cell.setHeader(true);
                    } else {
                        cell.setRole("VALUE");
                        cell.setHeader(false);
                    }
                }
            }
        }

        for (TableCellDto cell : cells) {
            if (cell.getRole() == null) {
                cell.setRole("VALUE");
                cell.setHeader(false);
            }
        }
    }

    /**
     * group 영향권이 아닌 일반 행을 단순 row/value로 처리
     */
    private void assignSimpleRowValuePattern(List<TableCellDto> row) {
        boolean firstMeaningfulAssigned = false;

        for (TableCellDto cell : row) {
            String text = normalizeCellText(cell.getText());

            if (text.isBlank()) {
                cell.setRole("VALUE");
                cell.setHeader(false);
                continue;
            }

            if (!firstMeaningfulAssigned) {
                cell.setRole("HEADER_ROW");
                cell.setHeader(true);
                firstMeaningfulAssigned = true;
            } else {
                cell.setRole("VALUE");
                cell.setHeader(false);
            }
        }
    }

    /**
     * 현재 row 가 어떤 GROUP 셀의 rowSpan 범위 안에 포함되는지 확인
     */
    private boolean isRowCoveredByAnyGroup(int rowIndex, List<TableCellDto> cells) {
        for (TableCellDto cell : cells) {
            if (!"HEADER_GROUP".equals(cell.getRole())) {
                continue;
            }

            int start = cell.getRowIndex();
            int end = cell.getRowIndex() + Math.max(1, cell.getRowSpan()) - 1;

            if (rowIndex >= start && rowIndex <= end) {
                return true;
            }
        }

        return false;
    }

    /**
     * 이 행에서 "row처럼 보이는" 스타일 시그니처를 찾는다.
     * 점수 높은 signature 중 가장 많이 등장한 것을 dominant row style 로 본다.
     */
    private String findDominantRowStyleSignature(List<TableCellDto> rowCells) {
        Map<String, Integer> countMap = new LinkedHashMap<>();
        Map<String, Integer> scoreMap = new LinkedHashMap<>();

        for (TableCellDto cell : rowCells) {
            String text = normalizeCellText(cell.getText());
            if (text.isBlank()) {
                continue;
            }

            String signature = buildStyleSignature(cell);
            int score = rowStyleScore(cell);

            if (score < 2) {
                continue;
            }

            countMap.put(signature, countMap.getOrDefault(signature, 0) + 1);
            scoreMap.put(signature, Math.max(scoreMap.getOrDefault(signature, 0), score));
        }

        String bestSignature = null;
        int bestCount = -1;
        int bestScore = -1;

        for (String signature : countMap.keySet()) {
            int count = countMap.getOrDefault(signature, 0);
            int score = scoreMap.getOrDefault(signature, 0);

            if (count > bestCount || (count == bestCount && score > bestScore)) {
                bestCount = count;
                bestScore = score;
                bestSignature = signature;
            }
        }

        return bestSignature;
    }

    private int countCellsWithSignature(List<TableCellDto> cells, String signature) {
        int count = 0;
        for (TableCellDto cell : cells) {
            if (signature.equals(buildStyleSignature(cell))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 반복형이 아닌 행에서 첫 번째 row 후보 찾기
     */
    private TableCellDto findFirstRowLikeCell(List<TableCellDto> cells) {
        TableCellDto best = null;
        int bestScore = Integer.MIN_VALUE;

        for (TableCellDto cell : cells) {
            String text = normalizeCellText(cell.getText());

            if (text.isBlank()) {
                continue;
            }

            int score = rowStyleScore(cell);

            if (best == null || score > bestScore) {
                best = cell;
                bestScore = score;
            }
        }

        // 스타일 점수 후보가 하나도 없으면 첫 의미 있는 셀로 fallback
        if (best == null) {
            for (TableCellDto cell : cells) {
                if (!normalizeCellText(cell.getText()).isBlank()) {
                    return cell;
                }
            }
        }

        return best;
    }

    /**
     * row 스타일 점수
     * - 회색 배경
     * - bold
     * - 짧은 텍스트
     * - 숫자 아님
     */
    private int rowStyleScore(TableCellDto cell) {
        int score = 0;

        String bg = normalizeColor(cell.getBackgroundColor());
        String text = normalizeCellText(cell.getText());

        if (isGrayLike(bg)) {
            score += 2;
        }
        if (Boolean.TRUE.equals(cell.getBold())) {
            score += 2;
        }
        if (!text.isBlank() && text.length() <= 15) {
            score += 1;
        }
        if (!looksNumeric(text)) {
            score += 1;
        }

        return score;
    }

    /**
     * 셀 스타일 시그니처
     * 혼합형에서 반복되는 row 셀 서체/배경을 묶기 위한 키
     */
    private String buildStyleSignature(TableCellDto cell) {
        String bg = normalizeColor(cell.getBackgroundColor());
        String bold = Boolean.TRUE.equals(cell.getBold()) ? "B1" : "B0";
        String italic = Boolean.TRUE.equals(cell.getItalic()) ? "I1" : "I0";
        String underline = Boolean.TRUE.equals(cell.getUnderline()) ? "U1" : "U0";
        String size = cell.getFontSize() == null ? "SZ?" : "SZ" + cell.getFontSize();
        String align = cell.getAlign() == null ? "AL?" : "AL" + cell.getAlign().trim().toUpperCase();
        String family = cell.getFontFamily() == null ? "FF?" : "FF" + cell.getFontFamily().trim();

        return bg + "|" + bold + "|" + italic + "|" + underline + "|" + size + "|" + align + "|" + family;
    }

    private boolean hasAnyRowSpanGreaterThanOne(List<TableCellDto> cells) {
        for (TableCellDto cell : cells) {
            if (cell.getRowSpan() > 1) {
                return true;
            }
        }
        return false;
    }

    private boolean isHorizontalHeaderLike(List<TableCellDto> cells) {
        List<TableCellDto> firstRow = getRowCellsSorted(cells, 0);
        if (firstRow.isEmpty()) return false;

        int shortTextCount = 0;
        int textCount = 0;

        for (TableCellDto cell : firstRow) {
            String text = normalizeCellText(cell.getText());
            if (text.isBlank()) continue;

            textCount++;
            if (text.length() <= 10 && !looksNumeric(text)) {
                shortTextCount++;
            }
        }

        return textCount > 0 && shortTextCount >= Math.max(1, textCount - 1);
    }

    private boolean isVerticalLabelValueLike(List<TableCellDto> cells, int rowCount) {
        int labelishRows = 0;
        int checkedRows = 0;

        for (int r = 0; r < rowCount; r++) {
            List<TableCellDto> row = getRowCellsSorted(cells, r);
            if (row.size() < 2) continue;

            TableCellDto first = row.get(0);
            TableCellDto second = row.get(1);

            String left = normalizeCellText(first.getText());
            String right = normalizeCellText(second.getText());

            if (left.isBlank() && right.isBlank()) continue;

            checkedRows++;
            if (!left.isBlank() && left.length() <= 15 && !looksNumeric(left)) {
                labelishRows++;
            }
        }

        return checkedRows > 0 && labelishRows >= Math.max(1, checkedRows / 2);
    }

    private boolean hasRepeatedRowValuePairs(List<TableCellDto> cells, int rowCount) {
        for (int r = 0; r < rowCount; r++) {
            List<TableCellDto> row = getRowCellsSorted(cells, r);
            if (row.size() < 4) continue;

            int pairLike = 0;
            for (int i = 0; i + 1 < row.size(); i += 2) {
                String left = normalizeCellText(row.get(i).getText());
                String right = normalizeCellText(row.get(i + 1).getText());

                boolean leftLabelish = !left.isBlank() && left.length() <= 15 && !looksNumeric(left);
                boolean rightValueish = right.isBlank() || looksNumeric(right) || right.length() >= left.length();

                if (leftLabelish && rightValueish) {
                    pairLike++;
                }
            }

            if (pairLike >= 2) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUniformBodyRows(List<TableCellDto> cells, int rowCount, int colCount) {
        if (rowCount < 2) return false;

        int matched = 0;
        int checked = 0;

        for (int r = 1; r < rowCount; r++) {
            List<TableCellDto> row = getRowCellsSorted(cells, r);
            if (row.isEmpty()) continue;

            checked++;
            if (row.size() == colCount) {
                matched++;
            }
        }

        return checked > 0 && matched >= Math.max(1, checked / 2);
    }

    private boolean isMostlyLeftLabelRightValue(List<TableCellDto> cells, int rowCount) {
        int matched = 0;
        int checked = 0;

        for (int r = 0; r < rowCount; r++) {
            List<TableCellDto> row = getRowCellsSorted(cells, r);
            if (row.size() < 2) continue;

            String left = normalizeCellText(row.get(0).getText());
            String right = normalizeCellText(row.get(1).getText());

            if (left.isBlank() && right.isBlank()) continue;

            checked++;
            boolean leftLabelish = !left.isBlank() && left.length() <= 15 && !looksNumeric(left);
            boolean rightValueish = right.isBlank() || looksNumeric(right) || right.length() >= left.length();

            if (leftLabelish && rightValueish) {
                matched++;
            }
        }

        return checked > 0 && matched >= Math.max(1, checked / 2);
    }

    private boolean looksNumeric(String text) {
        String t = normalizeCellText(text).replaceAll("\\s+", "");
        if (t.isBlank()) return false;

        return t.matches("[0-9,./()%+\\-]+");
    }

    private Map<Integer, List<TableCellDto>> groupCellsByRow(List<TableCellDto> cells) {
        Map<Integer, List<TableCellDto>> rows = new HashMap<>();

        for (TableCellDto cell : cells) {
            rows.computeIfAbsent(cell.getRowIndex(), k -> new ArrayList<>()).add(cell);
        }

        return rows;
    }

    private List<TableCellDto> getRowCellsSorted(List<TableCellDto> cells, int rowIndex) {
        List<TableCellDto> row = new ArrayList<>();
        for (TableCellDto cell : cells) {
            if (cell.getRowIndex() == rowIndex) {
                row.add(cell);
            }
        }
        row.sort(Comparator.comparingInt(TableCellDto::getColIndex));
        return row;
    }

    private String normalizeCellText(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * preview용 표 높이 추정
     *
     * 기본 아이디어:
     * 1. 각 행 높이를 개별 추정
     * 2. group 셀이 걸친 구간은 더 여유 있게
     * 3. 긴 설명문/줄바꿈 많은 행은 더 크게
     * 4. 혼합형 대형 폼 표는 페이지 높이의 일정 비율 이상을 최소 보장
     */
    
    private int estimatePreviewTableHeight(List<TableCellDto> cells, int rowCount, int colCount) {
        if (rowCount <= 0) {
            return 80;
        }

        int[] rowHeights = new int[rowCount];

        // 기본 행 높이
        for (int r = 0; r < rowCount; r++) {
            rowHeights[r] = 30;
        }

        for (TableCellDto cell : cells) {
            String text = normalizeCellText(cell.getText());
            int startRow = Math.max(0, cell.getRowIndex());
            int span = Math.max(1, cell.getRowSpan());
            int endRow = Math.min(rowCount - 1, startRow + span - 1);

            // 대략적인 줄 수 추정
            int explicitLines = countExplicitLines(cell.getText());
            int wrappedLines = estimateWrappedLines(text);
            int lineCount = Math.max(explicitLines, wrappedLines);

            int heightScore = 0;

            // 기본 텍스트 높이
            if (!text.isBlank()) {
                heightScore += 10;
            }

            // 줄 수 많을수록 더 키움
            if (lineCount >= 2) {
                heightScore += (lineCount - 1) * 8;
            }

            // 세로 병합 그룹 셀은 전체적으로 여유를 더 준다
            if (span > 1) {
                heightScore += 12;
            }

            // 긴 설명문 셀은 행 높이를 확실히 키운다
            if (text.length() >= 25) {
                heightScore += 10;
            }
            if (text.length() >= 50) {
                heightScore += 12;
            }

            // bold + 회색 배경 라벨 셀은 약간 높이 가산
            if (Boolean.TRUE.equals(cell.getBold())) {
                heightScore += 4;
            }
            if (isGrayLike(normalizeColor(cell.getBackgroundColor()))) {
                heightScore += 4;
            }

            // rowSpan == 1 이면 해당 행에 직접 반영
            if (span == 1) {
                rowHeights[startRow] = Math.max(rowHeights[startRow], 30 + heightScore);
            } else {
                // 여러 행에 걸친 group 셀은 해당 구간 전체 행에 조금씩 반영
                int bonusPerRow = Math.max(4, heightScore / span);
                for (int r = startRow; r <= endRow; r++) {
                    rowHeights[r] = Math.max(rowHeights[r], rowHeights[r] + bonusPerRow);
                }
            }
        }

        int estimatedHeight = 16; // table padding 느낌
        for (int h : rowHeights) {
            estimatedHeight += h;
        }

        // 혼합형 대형 폼 표면 preview에서 최소 높이 보장
        if (isLargeMixedFormTable(cells, rowCount, colCount)) {
            int minLargeFormHeight = (int) (PREVIEW_PAGE_HEIGHT * LARGE_FORM_MIN_HEIGHT_RATIO);
            estimatedHeight = Math.max(estimatedHeight, minLargeFormHeight);
        }

        return Math.max(80, estimatedHeight);
    }

    /**
     * preview에서 크게 보여줘야 하는 "대형 혼합 폼 표"인지 판별
     */
    private boolean isLargeMixedFormTable(List<TableCellDto> cells, int rowCount, int colCount) {
        int groupCellCount = 0;
        int longTextCellCount = 0;
        int boldGrayCellCount = 0;

        for (TableCellDto cell : cells) {
            String text = normalizeCellText(cell.getText());

            if (cell.getRowSpan() > 1) {
                groupCellCount++;
            }

            if (text.length() >= 20) {
                longTextCellCount++;
            }

            if (Boolean.TRUE.equals(cell.getBold())
                    && isGrayLike(normalizeColor(cell.getBackgroundColor()))) {
                boldGrayCellCount++;
            }
        }

        boolean manyRows = rowCount >= 10;
        boolean hasGroups = groupCellCount >= 2;
        boolean hasFormLikeLabels = boldGrayCellCount >= 5;
        boolean hasLongTexts = longTextCellCount >= 3;

        return manyRows && (hasGroups || hasFormLikeLabels || hasLongTexts);
    }

    private int countExplicitLines(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return 1;
        }
        return Math.max(1, rawText.split("\\r\\n|\\r|\\n").length);
    }

    /**
     * 아주 대략적인 줄바꿈 추정
     * preview 폭을 모르므로 텍스트 길이 기준으로만 계산
     */
    private int estimateWrappedLines(String text) {
        if (text == null || text.isBlank()) {
            return 1;
        }

        int len = text.length();

        if (len <= 10) return 1;
        if (len <= 22) return 2;
        if (len <= 40) return 3;
        if (len <= 65) return 4;
        return 5;
    }

    private String normalizeColor(String color) {
        if (color == null) {
            return "";
        }

        String v = color.trim().replace("#", "").toUpperCase();

        if (v.startsWith("FF") && v.length() == 8) {
            v = v.substring(2);
        }

        if (v.length() > 6) {
            v = v.substring(v.length() - 6);
        }

        return v;
    }

    private boolean isGrayLike(String rgb) {
        if (rgb == null || rgb.length() != 6) {
            return false;
        }

        try {
            int c1 = Integer.parseInt(rgb.substring(0, 2), 16);
            int c2 = Integer.parseInt(rgb.substring(2, 4), 16);
            int c3 = Integer.parseInt(rgb.substring(4, 6), 16);

            int max = Math.max(c1, Math.max(c2, c3));
            int min = Math.min(c1, Math.min(c2, c3));

            return (max - min) <= 12 && max >= 180 && max <= 245;
        } catch (Exception e) {
            return false;
        }
    }

    private void populateCellRelativeGeometry(TableNodeDto dto) {
        if (dto.getCells() == null || dto.getCells().isEmpty()) {
            return;
        }

        int colCount = dto.getColCount() != null ? dto.getColCount() : 1;
        int rowCount = dto.getRowCount() != null ? dto.getRowCount() : 1;
        double tableWidth = dto.getWidth() != null ? dto.getWidth() : 420.0;
        double tableHeight = dto.getHeight() != null ? dto.getHeight() : 120.0;

        double baseColWidth = tableWidth / Math.max(1, colCount);
        double baseRowHeight = tableHeight / Math.max(1, rowCount);

        double[] colLefts = new double[colCount];
        double[] colWidths = new double[colCount];

        for (int c = 0; c < colCount; c++) {
            colLefts[c] = c == 0 ? 0 : colLefts[c - 1] + colWidths[c - 1];
            if (c == colCount - 1) {
                colWidths[c] = tableWidth - colLefts[c];
            } else {
                colWidths[c] = Math.floor(baseColWidth);
            }
        }

        double[] rowTops = new double[rowCount];
        double[] rowHeights = new double[rowCount];

        for (int r = 0; r < rowCount; r++) {
            rowTops[r] = r == 0 ? 0 : rowTops[r - 1] + rowHeights[r - 1];
            if (r == rowCount - 1) {
                rowHeights[r] = tableHeight - rowTops[r];
            } else {
                rowHeights[r] = Math.floor(baseRowHeight);
            }
        }

        for (TableCellDto cell : dto.getCells()) {
            int startCol = Math.max(0, Math.min(colCount - 1, cell.getColIndex()));
            int startRow = Math.max(0, Math.min(rowCount - 1, cell.getRowIndex()));
            int endCol = Math.min(colCount - 1, startCol + Math.max(1, cell.getColSpan()) - 1);
            int endRow = Math.min(rowCount - 1, startRow + Math.max(1, cell.getRowSpan()) - 1);

            double x = colLefts[startCol];
            double y = rowTops[startRow];
            double width = (colLefts[endCol] + colWidths[endCol]) - x;
            double height = (rowTops[endRow] + rowHeights[endRow]) - y;

            cell.setX(x);
            cell.setY(y);
            cell.setWidth(width);
            cell.setHeight(height);
        }
    }

    private List<List<String>> buildCellTextsGrid(List<TableCellDto> cells, int rowCount, int colCount) {
        List<List<String>> grid = new ArrayList<>();

        for (int r = 0; r < rowCount; r++) {
            List<String> row = new ArrayList<>();
            for (int c = 0; c < colCount; c++) {
                row.add("");
            }
            grid.add(row);
        }

        for (TableCellDto cell : cells) {
            int r = cell.getRowIndex();
            int c = cell.getColIndex();

            if (r >= 0 && r < rowCount && c >= 0 && c < colCount) {
                grid.get(r).set(c, cell.getText() == null ? "" : cell.getText());
            }
        }

        return grid;
    }

    private int readSpan(Element el, String... attrNames) {
        String value = readAnyAttr(el, attrNames);

        if (value != null && !value.isBlank()) {
            try {
                return Math.max(1, Integer.parseInt(value.trim()));
            } catch (Exception ignored) {
            }
        }

        Integer nested = readNestedSpan(el, attrNames);
        return nested != null ? nested : 1;
    }

    private Integer readNestedSpan(Element el, String... attrNames) {
        if (el == null) {
            return null;
        }

        NodeList children = el.getElementsByTagName("*");
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element child)) {
                continue;
            }

            String value = readAnyAttr(child, attrNames);
            if (value != null && !value.isBlank()) {
                try {
                    return Math.max(1, Integer.parseInt(value.trim()));
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private Integer readIntAttr(Element el, String... names) {
        String value = readAnyAttr(el, names);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int findNextAvailableCol(
            Map<Integer, Map<Integer, Boolean>> occupied,
            int rowIndex,
            int startCol
    ) {
        int col = startCol;

        while (occupied
                .getOrDefault(rowIndex, Map.of())
                .getOrDefault(col, false)) {
            col++;
        }

        return col;
    }

    private void markOccupied(
            Map<Integer, Map<Integer, Boolean>> occupied,
            int rowIndex,
            int colIndex,
            int rowSpan,
            int colSpan
    ) {
        for (int r = rowIndex; r < rowIndex + rowSpan; r++) {
            Map<Integer, Boolean> rowMap = occupied.computeIfAbsent(r, k -> new HashMap<>());
            for (int c = colIndex; c < colIndex + colSpan; c++) {
                rowMap.put(c, true);
            }
        }
    }

    private Document loadHeaderDoc(ZipFile zipFile) {
        try {
            ZipEntry headerEntry = zipFile.getEntry("Contents/header.xml");
            if (headerEntry == null) {
                headerEntry = zipFile.getEntry("Contents/header0.xml");
            }
            if (headerEntry == null) {
                return null;
            }

            byte[] headerBytes;
            try (InputStream in = zipFile.getInputStream(headerEntry)) {
                headerBytes = in.readAllBytes();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);

            return factory.newDocumentBuilder()
                    .parse(new InputSource(new ByteArrayInputStream(headerBytes)));
        } catch (Exception e) {
            return null;
        }
    }

    private StyleProfileDto extractCellRepresentativeStyle(File hwpxFile, Element cellEl) {
        if (cellEl == null) {
            return null;
        }

        List<Element> paraList = getElements(cellEl, "hp:p");
        for (Element paraEl : paraList) {
            String paraPrIdRef = readAnyAttr(paraEl, "paraPrIDRef", "paraPrIdRef");

            List<Element> runList = getElements(paraEl, "hp:run");
            for (Element runEl : runList) {
                String charPrIdRef = readAnyAttr(runEl, "charPrIDRef", "charPrIdRef");

                if (paraPrIdRef != null && !paraPrIdRef.isBlank()
                        && charPrIdRef != null && !charPrIdRef.isBlank()) {

                    return hwpxStyleExtractor.extractStyle(
                            new HwpxStyleExtractor.FileWrapper(hwpxFile),
                            paraPrIdRef,
                            charPrIdRef
                    );
                }
            }
        }

        return null;
    }

    private ExtractedTableStyle extractTableStyle(File hwpxFile, Document headerDoc, Element tblEl) {
        ExtractedTableStyle style = new ExtractedTableStyle();

        List<Element> rows = getDirectChildElements(tblEl, "hp:tr", "tr");

        Element headerCell = findFirstCellOfRow(rows, 0);
        Element firstBodyCell = findFirstCellOfRow(rows, 1);
        Element middleBodyCell = findRepresentativeMiddleBodyCell(rows);
        Element lastBodyCell = findFirstCellOfLastBodyRow(rows);

        if (firstBodyCell == null) firstBodyCell = headerCell;
        if (middleBodyCell == null) middleBodyCell = firstBodyCell;
        if (lastBodyCell == null) lastBodyCell = middleBodyCell;

        style.setHeaderTextStyle(extractCellTextStyle(hwpxFile, headerCell));
        style.setBodyTextStyle(extractCellTextStyle(hwpxFile, firstBodyCell));

        String headerBorderFillId = readAnyAttr(headerCell, "borderFillIDRef", "borderFillIdRef");
        String firstBodyBorderFillId = readAnyAttr(firstBodyCell, "borderFillIDRef", "borderFillIdRef");
        String lastBodyBorderFillId = readAnyAttr(lastBodyCell, "borderFillIDRef", "borderFillIdRef");

        style.setHeaderBackgroundColor(parseBackgroundColorFromHeader(headerDoc, headerBorderFillId, "#FFFFFFFF"));
        style.setBodyBackgroundColor(parseBackgroundColorFromHeader(headerDoc, firstBodyBorderFillId, "#FFFFFFFF"));

        style.setHeaderOuterBorder(parseBorderStyleFromHeader(
                headerDoc, headerBorderFillId, "hh:leftBorder", "#000000", "0.12 mm", "SOLID"
        ));
        style.setHeaderInnerBorder(parseBorderStyleFromHeader(
                headerDoc, headerBorderFillId, "hh:topBorder", "#000000", "0.12 mm", "SOLID"
        ));

        style.setBodyOuterBorder(parseBorderStyleFromHeader(
                headerDoc, firstBodyBorderFillId, "hh:leftBorder", "#000000", "0.12 mm", "SOLID"
        ));

        style.setBodyInnerVerticalBorder(parseBorderStyleFromHeader(
                headerDoc, firstBodyBorderFillId, "hh:rightBorder", "#000000", "0.12 mm", "SOLID"
        ));

        style.setBodyInnerHorizontalBorder(parseBorderStyleFromHeader(
                headerDoc, firstBodyBorderFillId, "hh:bottomBorder", "#000000", "0.12 mm", "SOLID"
        ));

        style.setBodyFirstRowTopBorder(parseBorderStyleFromHeader(
                headerDoc, firstBodyBorderFillId, "hh:topBorder", "#000000", "0.12 mm", "SOLID"
        ));

        style.setBodyLastRowBottomBorder(parseBorderStyleFromHeader(
                headerDoc, lastBodyBorderFillId, "hh:bottomBorder", "#000000", "0.12 mm", "SOLID"
        ));

        return style;
    }

    private ExtractedTableStyle.CellTextStyle extractCellTextStyle(File hwpxFile, Element cellEl) {
        if (cellEl == null) {
            return null;
        }

        List<Element> paraList = getElements(cellEl, "hp:p");
        for (Element paraEl : paraList) {
            String paraPrIdRef = readAnyAttr(paraEl, "paraPrIDRef", "paraPrIdRef");

            List<Element> runList = getElements(paraEl, "hp:run");
            for (Element runEl : runList) {
                String charPrIdRef = readAnyAttr(runEl, "charPrIDRef", "charPrIdRef");

                if (paraPrIdRef != null && !paraPrIdRef.isBlank()
                        && charPrIdRef != null && !charPrIdRef.isBlank()) {

                    StyleProfileDto profile = hwpxStyleExtractor.extractStyle(
                            new HwpxStyleExtractor.FileWrapper(hwpxFile),
                            paraPrIdRef,
                            charPrIdRef
                    );

                    ExtractedTableStyle.CellTextStyle textStyle = new ExtractedTableStyle.CellTextStyle();
                    textStyle.setFontFamily(profile.getFontFamily());
                    textStyle.setFontSize(profile.getFontSize());
                    textStyle.setBold(profile.getBold());
                    textStyle.setItalic(profile.getItalic());
                    textStyle.setUnderline(profile.getUnderline());
                    textStyle.setFontColor(profile.getColor());
                    textStyle.setAlign(profile.getAlign());
                    return textStyle;
                }
            }
        }

        return null;
    }

    private String parseBackgroundColorFromHeader(
            Document headerDoc,
            String borderFillId,
            String defaultColor
    ) {
        if (headerDoc == null || borderFillId == null || borderFillId.isBlank()) {
            return defaultColor;
        }

        NodeList borderFillList = headerDoc.getElementsByTagName("hh:borderFill");
        for (int i = 0; i < borderFillList.getLength(); i++) {
            Node node = borderFillList.item(i);
            if (!(node instanceof Element bf)) continue;

            if (!borderFillId.equals(readAnyAttr(bf, "id"))) {
                continue;
            }

            NodeList winBrushList = bf.getElementsByTagName("hc:winBrush");
            if (winBrushList.getLength() > 0 && winBrushList.item(0) instanceof Element winBrush) {
                String faceColor = readAnyAttr(winBrush, "faceColor");
                if (faceColor != null && !faceColor.isBlank()) {
                    return faceColor;
                }
            }

            return defaultColor;
        }

        return defaultColor;
    }

    private ExtractedTableStyle.BorderStyle parseBorderStyleFromHeader(
            Document headerDoc,
            String borderFillId,
            String edgeTagName,
            String defaultColor,
            String defaultWidth,
            String defaultLineStyle
    ) {
        ExtractedTableStyle.BorderStyle style = new ExtractedTableStyle.BorderStyle();
        style.setColor(defaultColor);
        style.setWidth(defaultWidth);
        style.setLineStyle(defaultLineStyle);

        if (headerDoc == null || borderFillId == null || borderFillId.isBlank()) {
            return style;
        }

        NodeList borderFillList = headerDoc.getElementsByTagName("hh:borderFill");
        for (int i = 0; i < borderFillList.getLength(); i++) {
            Node node = borderFillList.item(i);
            if (!(node instanceof Element bf)) continue;

            if (!borderFillId.equals(readAnyAttr(bf, "id"))) {
                continue;
            }

            NodeList children = bf.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (!(child instanceof Element edgeEl)) continue;

                String nodeName = edgeEl.getNodeName();
                String localName = edgeEl.getLocalName();

                if (!edgeTagName.equalsIgnoreCase(nodeName) && !edgeTagName.equalsIgnoreCase(localName)) {
                    continue;
                }

                String type = readAnyAttr(edgeEl, "type");
                String width = readAnyAttr(edgeEl, "width");
                String color = readAnyAttr(edgeEl, "color");

                if (type != null && !type.isBlank()) {
                    style.setLineStyle(type);
                }
                if (width != null && !width.isBlank()) {
                    style.setWidth(width);
                }
                if (color != null && !color.isBlank()) {
                    style.setColor(color);
                }

                return style;
            }
        }

        return style;
    }

    private Element findFirstCellOfRow(List<Element> rows, int rowIndex) {
        if (rows == null || rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }

        Element rowEl = rows.get(rowIndex);
        List<Element> cellElements = getDirectChildElements(rowEl, "hp:tc", "tc");
        if (cellElements.isEmpty()) {
            return null;
        }

        return cellElements.get(0);
    }

    private Element findRepresentativeMiddleBodyCell(List<Element> rows) {
        if (rows == null || rows.size() < 3) {
            return findFirstCellOfRow(rows, 1);
        }

        if (rows.size() == 3) {
            return findFirstCellOfRow(rows, 1);
        }

        return findFirstCellOfRow(rows, 2);
    }

    private Element findFirstCellOfLastBodyRow(List<Element> rows) {
        if (rows == null || rows.size() < 2) {
            return null;
        }
        Element rowEl = rows.get(rows.size() - 1);
        List<Element> cellElements = getDirectChildElements(rowEl, "hp:tc", "tc");
        if (cellElements.isEmpty()) {
            return null;
        }
        return cellElements.get(0);
    }

    private List<ZipEntry> findSectionEntries(ZipFile zipFile) {
        List<ZipEntry> result = new ArrayList<>();

        zipFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().startsWith("Contents/section"))
                .filter(entry -> entry.getName().endsWith(".xml"))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .forEach(result::add);

        return result;
    }

    private List<Element> getElements(Element root, String tagName) {
        List<Element> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        NodeList list = root.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node instanceof Element el) {
                result.add(el);
            }
        }
        return result;
    }

    private List<Element> getDirectChildElements(Element root, String tagName, String localFallback) {
        List<Element> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        NodeList list = root.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (!(node instanceof Element el)) {
                continue;
            }

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            if (tagName.equalsIgnoreCase(nodeName) || localFallback.equalsIgnoreCase(localName)) {
                result.add(el);
            }
        }

        return result;
    }

    private String extractCellText(Element cellEl) {
        List<Element> paraList = getElements(cellEl, "hp:p");
        List<String> lines = new ArrayList<>();

        for (Element paraEl : paraList) {
            NodeList textNodes = paraEl.getElementsByTagName("hp:t");
            StringBuilder line = new StringBuilder();

            for (int i = 0; i < textNodes.getLength(); i++) {
                Node node = textNodes.item(i);
                String text = node.getTextContent();
                if (text != null) {
                    line.append(text);
                }
            }

            String normalized = line.toString().trim();
            if (!normalized.isEmpty()) {
                lines.add(normalized);
            }
        }

        return String.join("\n", lines);
    }

    private String readAnyAttr(Element el, String... names) {
        if (el == null || names == null) {
            return null;
        }

        NamedNodeMap attrs = el.getAttributes();
        if (attrs == null) {
            return null;
        }

        for (String name : names) {
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                if (attr == null) continue;

                String nodeName = attr.getNodeName();
                String localName = attr.getLocalName();

                if (name.equalsIgnoreCase(nodeName) || name.equalsIgnoreCase(localName)) {
                    return attr.getNodeValue();
                }
            }
        }

        return null;
    }
}