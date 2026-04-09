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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class HwpxTableExtractor {

    private final HwpxStyleExtractor hwpxStyleExtractor;

    public HwpxTableExtractor(HwpxStyleExtractor hwpxStyleExtractor) {
        this.hwpxStyleExtractor = hwpxStyleExtractor;
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

                    /**
                     * 병합 셀 점유 상태
                     * vertical merge / horizontal merge 모두 반영하기 위한 grid 점유표
                     */
                    Map<Integer, Map<Integer, Boolean>> occupied = new HashMap<>();

                    int maxColCount = 0;

                    for (int rowIndex = 0; rowIndex < rowElements.size(); rowIndex++) {
                        Element rowEl = rowElements.get(rowIndex);
                        List<Element> cellElements = getDirectChildElements(rowEl, "hp:tc", "tc");

                        int currentCol = 0;

                        for (Element cellEl : cellElements) {
                            Integer explicitRowAddr = readIntAttr(cellEl,
                                    "rowAddr", "rowaddr", "row", "rowIndex");
                            Integer explicitColAddr = readIntAttr(cellEl,
                                    "colAddr", "coladdr", "col", "colIndex");

                            int actualRow = explicitRowAddr != null ? explicitRowAddr : rowIndex;
                            currentCol = explicitColAddr != null
                                    ? explicitColAddr
                                    : findNextAvailableCol(occupied, actualRow, currentCol);

                            int rowSpan = readSpan(cellEl,
                                    "rowSpan", "rowspan", "rowSpanCount",
                                    "vertSpan", "vSpan", "rowMerge");
                            int colSpan = readSpan(cellEl,
                                    "colSpan", "colspan", "colSpanCount",
                                    "horzSpan", "hSpan", "colMerge");

                            String cellText = extractCellText(cellEl);

                            TableCellDto cell = new TableCellDto();
                            cell.setRowIndex(actualRow);
                            cell.setColIndex(currentCol);
                            cell.setRowSpan(rowSpan);
                            cell.setColSpan(colSpan);
                            cell.setText(cellText);
                            cell.setHeader(actualRow == 0);

                            String borderFillId = readAnyAttr(cellEl, "borderFillIDRef", "borderFillIdRef");
                            String backgroundColor = parseBackgroundColorFromHeader(
                                    headerDoc, borderFillId, null
                            );
                            cell.setBackgroundColor(backgroundColor);

                            cells.add(cell);

                            markOccupied(occupied, actualRow, currentCol, rowSpan, colSpan);
                            currentCol += colSpan;
                            maxColCount = Math.max(maxColCount, currentCol);
                        }
                    }

                    int rowCount = calculateRowCount(cells, rowElements.size());
                    int colCount = Math.max(1, maxColCount);

                    dto.setRowCount(rowCount);
                    dto.setColCount(colCount);
                    dto.setCells(cells);

                    // fallback 용 2차원 텍스트 그리드도 유지
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

                    /**
                     * 각 row의 총 가로 길이를 동일하게 맞추기 위해
                     * table 전체 width를 먼저 정하고,
                     * colCount 기준의 공통 그리드로 셀 상대좌표를 계산한다.
                     */
                    int tableWidth = Math.max(420, Math.min(560, colCount * 60));
                    int tableHeight = Math.max(80, rowCount * 44 + 16);

                    int pageHeightLimit = 680;
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

    private void populateCellRelativeGeometry(TableNodeDto dto) {
        if (dto.getCells() == null || dto.getCells().isEmpty()) {
            return;
        }

        int colCount = dto.getColCount() != null ? dto.getColCount() : 1;
        int rowCount = dto.getRowCount() != null ? dto.getRowCount() : 1;
        double tableWidth = dto.getWidth() != null ? dto.getWidth() : 420.0;
        double tableHeight = dto.getHeight() != null ? dto.getHeight() : 120.0;

        /**
         * 모든 row의 총 가로 길이를 같게 맞추기 위해
         * "공통 열 그리드" 기준으로 셀 좌표를 만든다.
         */
        double colUnit = tableWidth / Math.max(1, colCount);
        double rowUnit = tableHeight / Math.max(1, rowCount);

        for (TableCellDto cell : dto.getCells()) {
            double x = cell.getColIndex() * colUnit;
            double y = cell.getRowIndex() * rowUnit;
            double width = Math.max(colUnit, colUnit * Math.max(1, cell.getColSpan()));
            double height = Math.max(rowUnit, rowUnit * Math.max(1, cell.getRowSpan()));

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

        // 혹시 span 관련 하위 태그가 있을 경우 보조 탐색
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
        NodeList textNodes = cellEl.getElementsByTagName("hp:t");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < textNodes.getLength(); i++) {
            Node node = textNodes.item(i);
            String text = node.getTextContent();
            if (text != null) {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append(" ");
                    }
                    sb.append(trimmed);
                }
            }
        }

        return sb.toString().trim();
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