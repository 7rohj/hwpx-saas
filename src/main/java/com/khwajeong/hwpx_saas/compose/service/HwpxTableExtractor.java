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
                            String cellText = extractCellText(cellEl);
                            rowTexts.add(cellText);
                        }

                        maxColCount = Math.max(maxColCount, rowTexts.size());
                        cellTexts.add(rowTexts);
                    }

                    dto.setRowCount(cellTexts.size());
                    dto.setColCount(maxColCount);
                    dto.setCellTexts(cellTexts);

                    // 추가:
                    // 헤더/바디 구분 스타일 추출
                    ExtractedTableStyle extractedStyle = extractTableStyle(hwpxFile, tblEl);
                    dto.setExtractedStyle(extractedStyle);

                    // 기존 대표 스타일도 유지
                    if (extractedStyle != null) {
                        if (extractedStyle.getHeaderTextStyle() != null) {
                            dto.setFontFamily(extractedStyle.getHeaderTextStyle().getFontFamily());
                            dto.setFontSize(extractedStyle.getHeaderTextStyle().getFontSize());
                            dto.setBold(extractedStyle.getHeaderTextStyle().getBold());
                            dto.setItalic(extractedStyle.getHeaderTextStyle().getItalic());
                            dto.setFontColor(extractedStyle.getHeaderTextStyle().getFontColor());
                        }
                        dto.setBackgroundColor(extractedStyle.getHeaderBackgroundColor());

                        if (extractedStyle.getBodyOuterBorder() != null) {
                            dto.setBorderColor(extractedStyle.getBodyOuterBorder().getColor());
                            dto.setBorderWidth(extractedStyle.getBodyOuterBorder().getWidth());
                        }
                    }

                    // 추가:
                    // 임시 좌표 생성
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

    // 추가:
    // template 표에서 헤더/바디 스타일 세트 추출
    private ExtractedTableStyle extractTableStyle(File hwpxFile, Element tblEl) {
        ExtractedTableStyle style = new ExtractedTableStyle();

        List<Element> rows = getElements(tblEl, "hp:tr");

        Element headerCell = findFirstCellOfRow(rows, 0);
        Element bodyCell = findFirstCellOfRow(rows, 1);

        // body가 없으면 header 다음 fallback
        if (bodyCell == null) {
            bodyCell = headerCell;
        }

        // 추가:
        // 텍스트 스타일 추출
        style.setHeaderTextStyle(extractCellTextStyle(hwpxFile, headerCell));
        style.setBodyTextStyle(extractCellTextStyle(hwpxFile, bodyCell));

        // 추가:
        // 배경색 추출 (지금은 약식, 다음 단계에서 borderFill 정밀 파싱 가능)
        style.setHeaderBackgroundColor(findRoughBackgroundColor(headerCell, "#f1f5f9"));
        style.setBodyBackgroundColor(findRoughBackgroundColor(bodyCell, "#ffffff"));

        // 추가:
        // 헤더/바디 외곽선 / 내부선 스타일 분리
        style.setHeaderOuterBorder(extractRoughBorderStyle(headerCell, true, "#334155", 1, "solid"));
        style.setHeaderInnerBorder(extractRoughBorderStyle(headerCell, false, "#94a3b8", 1, "solid"));

        style.setBodyOuterBorder(extractRoughBorderStyle(bodyCell, true, "#64748b", 1, "solid"));
        style.setBodyInnerBorder(extractRoughBorderStyle(bodyCell, false, "#cbd5e1", 1, "solid"));

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

    // 추가:
    // 현재는 borderFill 정밀 파싱 전이라 fallback 기반으로 채움
    // 다음 단계에서 cell/table의 borderFillIDRef를 읽어 진짜 값으로 교체 가능
    private ExtractedTableStyle.BorderStyle extractRoughBorderStyle(
            Element cellEl,
            boolean outer,
            String defaultColor,
            Integer defaultWidth,
            String defaultLineStyle
    ) {
        ExtractedTableStyle.BorderStyle borderStyle = new ExtractedTableStyle.BorderStyle();
        borderStyle.setColor(defaultColor);
        borderStyle.setWidth(defaultWidth);
        borderStyle.setLineStyle(defaultLineStyle);

        if (cellEl == null) {
            return borderStyle;
        }

        // 다음 단계 확장 포인트:
        // borderFillIDRef / fillIDRef 같은 실제 HWPX 속성 읽어서 반영 가능
        return borderStyle;
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

    private String findRoughBackgroundColor(Element cellEl, String defaultColor) {
        if (cellEl == null) {
            return defaultColor;
        }

        // 다음 단계 확장 포인트:
        // 실제 fillBrush / borderFill 파싱
        return defaultColor;
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
                if (attr == null) {
                    continue;
                }

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