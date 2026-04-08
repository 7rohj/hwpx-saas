package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
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
import java.util.List;
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

                    List<Element> rowElements = getElements(tblEl, "hp:tr");
                    List<List<String>> cellTexts = new ArrayList<>();

                    int maxColCount = 0;
                    for (Element rowEl : rowElements) {
                        List<Element> cellElements = getElements(rowEl, "hp:tc");
                        List<String> rowTexts = new ArrayList<>();

                        for (Element cellEl : cellElements) {
                            rowTexts.add(extractCellText(cellEl));
                        }

                        maxColCount = Math.max(maxColCount, rowTexts.size());
                        cellTexts.add(rowTexts);
                    }

                    dto.setRowCount(cellTexts.size());
                    dto.setColCount(maxColCount);
                    dto.setCellTexts(cellTexts);

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

                    int rowCount = dto.getRowCount() != null ? dto.getRowCount() : 1;
                    int colCount = dto.getColCount() != null ? dto.getColCount() : 1;

                    int baseX = 56;
                    int baseY = virtualTopCursor;
                    int tableWidth = Math.max(320, Math.min(520, colCount * 110));
                    int tableHeight = Math.max(80, rowCount * 44 + 16);

                    int pageHeightLimit = 680;
                    int page = (baseY / pageHeightLimit) + 1;
                    int normalizedTop = baseY % pageHeightLimit;

                    dto.setPage(page);
                    dto.setX((double) baseX);
                    dto.setY((double) normalizedTop);
                    dto.setWidth((double) tableWidth);
                    dto.setHeight((double) tableHeight);

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

        List<Element> rows = getElements(tblEl, "hp:tr");

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
        String middleBodyBorderFillId = readAnyAttr(middleBodyCell, "borderFillIDRef", "borderFillIdRef");
        String lastBodyBorderFillId = readAnyAttr(lastBodyCell, "borderFillIDRef", "borderFillIdRef");

        style.setHeaderBackgroundColor(parseBackgroundColorFromHeader(headerDoc, headerBorderFillId, "#FFFFFFFF"));
        style.setBodyBackgroundColor(parseBackgroundColorFromHeader(headerDoc, firstBodyBorderFillId, "#FFFFFFFF"));

        // 헤더
        style.setHeaderOuterBorder(parseBorderStyleFromHeader(
                headerDoc, headerBorderFillId, "hh:leftBorder", "#000000", "0.12 mm", "SOLID"
        ));
        style.setHeaderInnerBorder(parseBorderStyleFromHeader(
                headerDoc, headerBorderFillId, "hh:topBorder", "#000000", "0.12 mm", "SOLID"
        ));

        // 바디 외부선
        style.setBodyOuterBorder(parseBorderStyleFromHeader(
                headerDoc, middleBodyBorderFillId, "hh:leftBorder", "#000000", "0.12 mm", "SOLID"
        ));

        // 핵심:
        // 표 바디 내부 세로선 = 첫 번째 바디 셀의 rightBorder
        style.setBodyInnerVerticalBorder(parseBorderStyleFromHeader(
                headerDoc, firstBodyBorderFillId, "hh:rightBorder", "#000000", "0.12 mm", "SOLID"
        ));

        // 표 바디 내부 가로선 = 첫 번째 바디 셀의 bottomBorder
        style.setBodyInnerHorizontalBorder(parseBorderStyleFromHeader(
                headerDoc, firstBodyBorderFillId, "hh:bottomBorder", "#000000", "0.12 mm", "SOLID"
        ));

        // 첫 번째 바디 row의 top
        style.setBodyFirstRowTopBorder(parseBorderStyleFromHeader(
                headerDoc, firstBodyBorderFillId, "hh:topBorder", "#000000", "0.12 mm", "SOLID"
        ));

        // 마지막 바디 row의 bottom
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
        List<Element> cellElements = getElements(rowEl, "hp:tc");
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
        List<Element> cellElements = getElements(rowEl, "hp:tc");
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

    private String extractCellText(Element cellEl) {
        NodeList textNodes = cellEl.getElementsByTagName("hp:t");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < textNodes.getLength(); i++) {
            Node node = textNodes.item(i);
            String text = node.getTextContent();
            if (text != null) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(text.trim());
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