package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.TableCellDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
import com.khwajeong.hwpx_saas.compose.model.ExtractedTableStyle;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class ComposeTableStyleApplyService {

    private final HwpxStyleRegistryService hwpxStyleRegistryService;

    public ComposeTableStyleApplyService(HwpxStyleRegistryService hwpxStyleRegistryService) {
        this.hwpxStyleRegistryService = hwpxStyleRegistryService;
    }

    public Path applyTableStyle(
            Path sourceHwpxPath,
            String sourceTableId,
            TableNodeDto templateTable
    ) throws Exception {
        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }
        if (templateTable == null) {
            throw new IllegalArgumentException("templateTable is required");
        }
        if (sourceTableId == null || sourceTableId.isBlank()) {
            throw new IllegalArgumentException("sourceTableId is required");
        }

        int targetTableIndex = parseTargetTableIndex(sourceTableId);
        if (targetTableIndex < 0) {
            throw new IllegalArgumentException("invalid sourceTableId: " + sourceTableId);
        }

        Path outputPath = Files.createTempFile("compose-table-applied-", ".hwpx");

        try (ZipFile zipFile = new ZipFile(sourceHwpxPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {

            Map<String, byte[]> updatedEntries = new LinkedHashMap<>();
            Map<String, Document> headerDocs = loadHeaderDocs(zipFile);

            Map<String, Map<String, RegisteredCellStyle>> registeredByHeader =
                    registerStylesToHeaders(headerDocs, templateTable);

            int[] currentTableIndexRef = new int[]{0};

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    updatedEntries.put(entry.getName(), new byte[0]);
                    continue;
                }

                byte[] bytes;
                try (InputStream in = zipFile.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }

                if (entry.getName().startsWith("Contents/section") && entry.getName().endsWith(".xml")) {
                    String headerName = toHeaderName(entry.getName());
                    Map<String, RegisteredCellStyle> registeredCellMap = registeredByHeader.get(headerName);

                    if (registeredCellMap == null && registeredByHeader.containsKey("Contents/header0.xml")) {
                        registeredCellMap = registeredByHeader.get("Contents/header0.xml");
                    }
                    if (registeredCellMap == null && registeredByHeader.containsKey("Contents/header.xml")) {
                        registeredCellMap = registeredByHeader.get("Contents/header.xml");
                    }

                    if (registeredCellMap != null) {
                        bytes = applyTableStyleToTargetTableInSectionXml(
                                bytes,
                                targetTableIndex,
                                currentTableIndexRef,
                                registeredCellMap
                        );
                    }
                }

                updatedEntries.put(entry.getName(), bytes);
            }

            for (Map.Entry<String, Document> e : headerDocs.entrySet()) {
                updatedEntries.put(e.getKey(), documentToBytes(e.getValue()));
            }

            for (Map.Entry<String, byte[]> e : updatedEntries.entrySet()) {
                ZipEntry newEntry = new ZipEntry(e.getKey());
                zos.putNextEntry(newEntry);
                if (e.getValue() != null && e.getValue().length > 0) {
                    zos.write(e.getValue());
                }
                zos.closeEntry();
            }
        }

        return outputPath;
    }

    private Map<String, Document> loadHeaderDocs(ZipFile zipFile) throws Exception {
        Map<String, Document> result = new LinkedHashMap<>();

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            if (!entry.getName().startsWith("Contents/header")) continue;
            if (!entry.getName().endsWith(".xml")) continue;

            try (InputStream in = zipFile.getInputStream(entry)) {
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(in);
                result.put(entry.getName(), doc);
            }
        }

        return result;
    }

    /**
     * header.xml 별로 template 셀 스타일을 등록한다.
     * key = "row:col"
     */
    private Map<String, Map<String, RegisteredCellStyle>> registerStylesToHeaders(
            Map<String, Document> headerDocs,
            TableNodeDto templateTable
    ) {
        Map<String, Map<String, RegisteredCellStyle>> result = new LinkedHashMap<>();
        Map<String, TableCellDto> templateCellMap = buildTemplateCellMap(templateTable.getCells());

        for (Map.Entry<String, Document> e : headerDocs.entrySet()) {
            Document doc = e.getValue();
            Map<String, RegisteredCellStyle> registeredCellMap = new LinkedHashMap<>();

            for (Map.Entry<String, TableCellDto> cellEntry : templateCellMap.entrySet()) {
                TableCellDto templateCell = cellEntry.getValue();

                ExtractedTableStyle.CellTextStyle textStyle = toCellTextStyle(templateCell);

                // char/para style 등록
                HwpxStyleRegistryService.RegisteredStyleIds ids =
                        hwpxStyleRegistryService.registerTableCellStyle(
                                doc,
                                textStyle,
                                hiddenBorder(),
                                hiddenBorder(),
                                templateCell.getBackgroundColor()
                        );

                // 4방향 borderFill 등록
                String borderFillId = hwpxStyleRegistryService.registerCustomBorderFill(
                        doc,
                        toBorderStyle(
                                templateCell.getBorderLeftStyle(),
                                templateCell.getBorderLeftColor(),
                                templateCell.getBorderLeftWidth()
                        ),
                        toBorderStyle(
                                templateCell.getBorderRightStyle(),
                                templateCell.getBorderRightColor(),
                                templateCell.getBorderRightWidth()
                        ),
                        toBorderStyle(
                                templateCell.getBorderTopStyle(),
                                templateCell.getBorderTopColor(),
                                templateCell.getBorderTopWidth()
                        ),
                        toBorderStyle(
                                templateCell.getBorderBottomStyle(),
                                templateCell.getBorderBottomColor(),
                                templateCell.getBorderBottomWidth()
                        ),
                        templateCell.getBackgroundColor()
                );

                RegisteredCellStyle registered = new RegisteredCellStyle();
                registered.ids = ids;
                registered.borderFillId = borderFillId;
                registered.fontSize = templateCell.getFontSize() != null && templateCell.getFontSize() > 0
                        ? templateCell.getFontSize()
                        : 10;

                registeredCellMap.put(cellEntry.getKey(), registered);
            }

            result.put(e.getKey(), registeredCellMap);
        }

        return result;
    }

    private ExtractedTableStyle.BorderStyle hiddenBorder() {
        ExtractedTableStyle.BorderStyle border = new ExtractedTableStyle.BorderStyle();
        border.setLineStyle("NONE");
        border.setWidth("0.1 mm");
        border.setColor("#000000");
        return border;
    }

    private ExtractedTableStyle.BorderStyle toBorderStyle(String lineStyle, String color, String width) {
        ExtractedTableStyle.BorderStyle border = new ExtractedTableStyle.BorderStyle();
        border.setLineStyle(
                lineStyle != null && !lineStyle.isBlank()
                        ? lineStyle
                        : "NONE"
        );
        border.setColor(
                color != null && !color.isBlank()
                        ? color
                        : "#000000"
        );
        border.setWidth(
                width != null && !width.isBlank()
                        ? width
                        : "0.12 mm"
        );
        return border;
    }

    private ExtractedTableStyle.CellTextStyle toCellTextStyle(TableCellDto templateCell) {
        ExtractedTableStyle.CellTextStyle textStyle = new ExtractedTableStyle.CellTextStyle();
        textStyle.setFontFamily(templateCell.getFontFamily());
        textStyle.setFontSize(templateCell.getFontSize());
        textStyle.setBold(templateCell.getBold());
        textStyle.setItalic(templateCell.getItalic());
        textStyle.setUnderline(templateCell.getUnderline());
        textStyle.setFontColor(templateCell.getFontColor());
        textStyle.setAlign(templateCell.getAlign());
        return textStyle;
    }

    private Map<String, TableCellDto> buildTemplateCellMap(List<TableCellDto> cells) {
        Map<String, TableCellDto> map = new LinkedHashMap<>();
        if (cells == null) {
            return map;
        }

        for (TableCellDto cell : cells) {
            map.put(cell.getRowIndex() + ":" + cell.getColIndex(), cell);
        }

        return map;
    }

    private byte[] applyTableStyleToTargetTableInSectionXml(
            byte[] sectionXmlBytes,
            int targetTableIndex,
            int[] currentTableIndexRef,
            Map<String, RegisteredCellStyle> registeredCellMap
    ) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(sectionXmlBytes)));

        NodeList tblList = doc.getElementsByTagName("hp:tbl");

        for (int i = 0; i < tblList.getLength(); i++) {
            Node node = tblList.item(i);
            if (!(node instanceof Element tblEl)) {
                continue;
            }

            int currentIndex = currentTableIndexRef[0];
            if (currentIndex == targetTableIndex) {
                applyStyleToTable(tblEl, registeredCellMap);
            }

            currentTableIndexRef[0] = currentIndex + 1;
        }

        return documentToBytes(doc);
    }

    /**
     * source table의 각 셀(row,col)에 대응하는 template cell 스타일 적용
     */
    private void applyStyleToTable(
            Element tblEl,
            Map<String, RegisteredCellStyle> registeredCellMap
    ) {
        List<Element> rows = getElements(tblEl, "hp:tr");
        Map<Integer, Map<Integer, Boolean>> occupied = new HashMap<>();

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Element rowEl = rows.get(rowIndex);
            List<Element> cells = getElements(rowEl, "hp:tc");

            int currentCol = 0;

            for (Element cellEl : cells) {
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

                RegisteredCellStyle registered = registeredCellMap.get(actualRow + ":" + currentCol);
                if (registered != null) {
                    applyCellFullStyle(
                            cellEl,
                            registered.ids,
                            registered.fontSize,
                            registered.borderFillId
                    );
                }

                markOccupied(occupied, actualRow, currentCol, rowSpan, colSpan);
                currentCol += colSpan;
            }
        }
    }

    private void applyCellFullStyle(
            Element cellEl,
            HwpxStyleRegistryService.RegisteredStyleIds ids,
            int fontSize,
            String overrideBorderFillId
    ) {
        if (cellEl == null || ids == null) {
            return;
        }

        String finalBorderFillId = (overrideBorderFillId != null && !overrideBorderFillId.isBlank())
                ? overrideBorderFillId
                : ids.getBorderFillId();

        if (finalBorderFillId != null && !finalBorderFillId.isBlank()) {
            cellEl.setAttribute("borderFillIDRef", finalBorderFillId);
        }

        List<Element> paraList = getElements(cellEl, "hp:p");
        for (Element paraEl : paraList) {
            if (ids.getParaPrId() != null && !ids.getParaPrId().isBlank()) {
                paraEl.setAttribute("paraPrIDRef", ids.getParaPrId());
            }

            List<Element> runList = getElements(paraEl, "hp:run");
            for (Element runEl : runList) {
                if (ids.getCharPrId() != null && !ids.getCharPrId().isBlank()) {
                    runEl.setAttribute("charPrIDRef", ids.getCharPrId());
                }
            }

            recalculateLineSegArray(paraEl, fontSize);
        }
    }

    private void recalculateLineSegArray(Element paraEl, int fontSize) {
        if (paraEl == null) {
            return;
        }

        int safeFontSize = fontSize > 0 ? fontSize : 10;

        int textHeight = safeFontSize * 100;
        int vertSize = textHeight;
        int baseline = (int) Math.round(textHeight * 0.85);
        int spacing = (int) Math.round(textHeight * 0.60);

        Element lineSegArray = findDirectChild(paraEl, "hp:linesegarray");
        if (lineSegArray == null) {
            lineSegArray = paraEl.getOwnerDocument().createElement("hp:linesegarray");
            paraEl.appendChild(lineSegArray);
        }

        List<Element> lineSegs = getDirectChildren(lineSegArray, "hp:lineseg");
        if (lineSegs.isEmpty()) {
            Element newLineSeg = paraEl.getOwnerDocument().createElement("hp:lineseg");
            newLineSeg.setAttribute("textpos", "0");
            newLineSeg.setAttribute("vertpos", "0");
            newLineSeg.setAttribute("vertsize", String.valueOf(vertSize));
            newLineSeg.setAttribute("textheight", String.valueOf(textHeight));
            newLineSeg.setAttribute("baseline", String.valueOf(baseline));
            newLineSeg.setAttribute("spacing", String.valueOf(spacing));
            newLineSeg.setAttribute("horzpos", "0");
            newLineSeg.setAttribute("horzsize", "23500");
            newLineSeg.setAttribute("flags", "393216");
            lineSegArray.appendChild(newLineSeg);
            return;
        }

        for (Element lineSeg : lineSegs) {
            lineSeg.setAttribute("textheight", String.valueOf(textHeight));
            lineSeg.setAttribute("vertsize", String.valueOf(vertSize));
            lineSeg.setAttribute("baseline", String.valueOf(baseline));
            lineSeg.setAttribute("spacing", String.valueOf(spacing));
        }
    }

    private Element findDirectChild(Element parent, String tagName) {
        if (parent == null) return null;

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            if (tagName.equalsIgnoreCase(nodeName) || tagName.equalsIgnoreCase(localName)) {
                return el;
            }
        }
        return null;
    }

    private List<Element> getDirectChildren(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        if (parent == null) return result;

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            if (tagName.equalsIgnoreCase(nodeName) || tagName.equalsIgnoreCase(localName)) {
                result.add(el);
            }
        }
        return result;
    }

    private String toHeaderName(String sectionName) {
        String headerName = sectionName.replace("Contents/section", "Contents/header");
        if (headerName.equals(sectionName)) {
            return "Contents/header0.xml";
        }
        return headerName;
    }

    private List<Element> getElements(Element root, String tagName) {
        List<Element> result = new ArrayList<>();
        if (root == null) return result;

        NodeList list = root.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node instanceof Element el) {
                result.add(el);
            }
        }
        return result;
    }

    private int parseTargetTableIndex(String sourceTableId) {
        if (sourceTableId == null || sourceTableId.isBlank()) {
            return -1;
        }
        if (!sourceTableId.startsWith("t")) {
            return -1;
        }
        try {
            return Integer.parseInt(sourceTableId.substring(1));
        } catch (Exception e) {
            return -1;
        }
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

    private String readAnyAttr(Element el, String... names) {
        if (el == null || names == null) {
            return null;
        }

        if (el.getAttributes() == null) {
            return null;
        }

        for (String name : names) {
            Node node = el.getAttributes().getNamedItem(name);
            if (node != null) {
                return node.getNodeValue();
            }

            for (int i = 0; i < el.getAttributes().getLength(); i++) {
                Node attr = el.getAttributes().item(i);
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

    private byte[] documentToBytes(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    private static class RegisteredCellStyle {
        private HwpxStyleRegistryService.RegisteredStyleIds ids;
        private String borderFillId;
        private int fontSize;
    }
}