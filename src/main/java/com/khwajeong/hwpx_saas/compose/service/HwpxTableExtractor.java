package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
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

                    // 행 추출
                    List<Element> rowElements = getDirectOrNestedElements(tblEl, "hp:tr");
                    List<List<String>> cellTexts = new ArrayList<>();

                    int maxColCount = 0;

                    for (Element rowEl : rowElements) {
                        List<Element> cellElements = getDirectOrNestedElements(rowEl, "hp:tc");
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

                    // 대표 스타일 추출 (첫 번째 셀 기준)
                    CellStyleRef firstCellStyleRef = findFirstCellStyleRef(tblEl);
                    if (firstCellStyleRef != null
                            && firstCellStyleRef.getParaPrIdRef() != null
                            && firstCellStyleRef.getCharPrIdRef() != null) {

                        StyleProfileDto style = hwpxStyleExtractor.extractStyle(
                                new HwpxStyleExtractor.FileWrapper(hwpxFile),
                                firstCellStyleRef.getParaPrIdRef(),
                                firstCellStyleRef.getCharPrIdRef()
                        );

                        dto.setFontFamily(style.getFontFamily());
                        dto.setFontSize(style.getFontSize());
                        dto.setBold(style.getBold());
                        dto.setItalic(style.getItalic());
                        dto.setFontColor(style.getColor());
                    }

                    // 배경색/테두리 MVP는 일단 약식
                    // 실제 hwpx table/cell border-fill을 정밀 분석하기 전까지 기본값 또는 null
                    dto.setBorderColor("#cbd5e1");
                    dto.setBorderWidth(1);
                    dto.setBackgroundColor(findRoughBackgroundColor(tblEl));

                    // 임시 좌표 계산
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

    private List<Element> getDirectOrNestedElements(Element root, String tagName) {
        List<Element> result = new ArrayList<>();
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

    private CellStyleRef findFirstCellStyleRef(Element tblEl) {
        NodeList cellList = tblEl.getElementsByTagName("hp:tc");

        for (int i = 0; i < cellList.getLength(); i++) {
            Node cellNode = cellList.item(i);
            if (!(cellNode instanceof Element cellEl)) {
                continue;
            }

            NodeList paraList = cellEl.getElementsByTagName("hp:p");
            for (int p = 0; p < paraList.getLength(); p++) {
                Node paraNode = paraList.item(p);
                if (!(paraNode instanceof Element paraEl)) {
                    continue;
                }

                String paraPrIdRef = readAnyAttr(paraEl, "paraPrIDRef", "paraPrIdRef");

                NodeList runList = paraEl.getElementsByTagName("hp:run");
                String charPrIdRef = null;

                for (int r = 0; r < runList.getLength(); r++) {
                    Node runNode = runList.item(r);
                    if (runNode instanceof Element runEl) {
                        charPrIdRef = readAnyAttr(runEl, "charPrIDRef", "charPrIdRef");
                        if (charPrIdRef != null && !charPrIdRef.isBlank()) {
                            break;
                        }
                    }
                }

                if (paraPrIdRef != null && !paraPrIdRef.isBlank()
                        && charPrIdRef != null && !charPrIdRef.isBlank()) {
                    return new CellStyleRef(paraPrIdRef, charPrIdRef);
                }
            }
        }

        return null;
    }

    private String findRoughBackgroundColor(Element tblEl) {
        // MVP:
        // 실제 borderFill / fillBrush 정밀 분석 대신 null 또는 기본색 반환
        // 다음 단계에서 cell/row 단위 배경색 분석 확장 예정
        return "#ffffff";
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

    private static class CellStyleRef {
        private final String paraPrIdRef;
        private final String charPrIdRef;

        public CellStyleRef(String paraPrIdRef, String charPrIdRef) {
            this.paraPrIdRef = paraPrIdRef;
            this.charPrIdRef = charPrIdRef;
        }

        public String getParaPrIdRef() {
            return paraPrIdRef;
        }

        public String getCharPrIdRef() {
            return charPrIdRef;
        }
    }
}