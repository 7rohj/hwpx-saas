package com.khwajeong.hwpx_saas.compose.service;

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
            ExtractedTableStyle style
    ) throws Exception {
        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }
        if (style == null) {
            throw new IllegalArgumentException("style is required");
        }

        Path outputPath = Files.createTempFile("compose-table-applied-", ".hwpx");

        try (ZipFile zipFile = new ZipFile(sourceHwpxPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {

            Map<String, byte[]> updatedEntries = new LinkedHashMap<>();
            Map<String, Document> headerDocs = loadHeaderDocs(zipFile);

            Map<String, RegisteredPair> registeredByHeader = registerStylesToHeaders(headerDocs, style);

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
                    RegisteredPair pair = registeredByHeader.get(headerName);

                    if (pair == null && registeredByHeader.containsKey("Contents/header0.xml")) {
                        pair = registeredByHeader.get("Contents/header0.xml");
                    }
                    if (pair == null && registeredByHeader.containsKey("Contents/header.xml")) {
                        pair = registeredByHeader.get("Contents/header.xml");
                    }

                    if (pair != null) {
                        bytes = applyTableStyleToAllTablesInSectionXml(bytes, pair);
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

    private Map<String, RegisteredPair> registerStylesToHeaders(
            Map<String, Document> headerDocs,
            ExtractedTableStyle style
    ) {
        Map<String, RegisteredPair> result = new LinkedHashMap<>();

        for (Map.Entry<String, Document> e : headerDocs.entrySet()) {
            Document doc = e.getValue();

            HwpxStyleRegistryService.RegisteredStyleIds headerIds =
                    hwpxStyleRegistryService.registerTableCellStyle(
                            doc,
                            style.getHeaderTextStyle(),
                            style.getHeaderOuterBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderBackgroundColor()
                    );

            HwpxStyleRegistryService.RegisteredStyleIds bodyIds =
                    hwpxStyleRegistryService.registerTableCellStyle(
                            doc,
                            style.getBodyTextStyle(),
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            // column = 0 전용 body borderFill
            String bodyFirstRowFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyOuterBorder(),           // left = 바디 처음칸 left
                            style.getBodyInnerVerticalBorder(),   // right = 내부 세로선
                            style.getBodyFirstRowTopBorder(),     // top
                            style.getBodyInnerHorizontalBorder(), // bottom
                            style.getBodyBackgroundColor()
                    );

            String bodyMiddleRowFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyOuterBorder(),           // left = 바디 외부 세로선
                            style.getBodyInnerVerticalBorder(),   // right = 내부 세로선
                            hiddenBorder(),                       // top 숨김
                            style.getBodyInnerHorizontalBorder(), // bottom
                            style.getBodyBackgroundColor()
                    );

            String bodyLastRowFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyOuterBorder(),             // left = 바디 외부 세로선
                            style.getBodyInnerVerticalBorder(),     // right = 내부 세로선
                            hiddenBorder(),                         // top 숨김
                            style.getBodyLastRowBottomBorder(),     // bottom
                            style.getBodyBackgroundColor()
                    );

            String bodySingleRowFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyOuterBorder(),             // left = 바디 외부 세로선
                            style.getBodyInnerVerticalBorder(),     // right = 내부 세로선
                            style.getBodyFirstRowTopBorder(),       // top
                            style.getBodyLastRowBottomBorder(),     // bottom
                            style.getBodyBackgroundColor()
                    );

            // 중간 column 전용 body borderFill
            String bodyFirstRowOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),   // left = 내부 세로선
                            style.getBodyInnerVerticalBorder(),   // right = 내부 세로선
                            style.getBodyFirstRowTopBorder(),     // top
                            style.getBodyInnerHorizontalBorder(), // bottom
                            style.getBodyBackgroundColor()
                    );

            String bodyMiddleRowOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),   // left = 내부 세로선
                            style.getBodyInnerVerticalBorder(),   // right = 내부 세로선
                            hiddenBorder(),                       // top 숨김
                            style.getBodyInnerHorizontalBorder(), // bottom
                            style.getBodyBackgroundColor()
                    );

            String bodyLastRowOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),   // left = 내부 세로선
                            style.getBodyInnerVerticalBorder(),   // right = 내부 세로선
                            hiddenBorder(),                       // top 숨김
                            style.getBodyLastRowBottomBorder(),   // bottom
                            style.getBodyBackgroundColor()
                    );

            String bodySingleRowOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),   // left = 내부 세로선
                            style.getBodyInnerVerticalBorder(),   // right = 내부 세로선
                            style.getBodyFirstRowTopBorder(),     // top
                            style.getBodyLastRowBottomBorder(),   // bottom
                            style.getBodyBackgroundColor()
                    );

            // 마지막 column 전용 body borderFill
            String bodyFirstRowLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),   // left = 내부 세로선
                            style.getBodyOuterBorder(),           // right = 바디 외부 세로선
                            style.getBodyFirstRowTopBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyMiddleRowLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),   // left = 내부 세로선
                            style.getBodyOuterBorder(),           // right = 바디 외부 세로선
                            hiddenBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyLastRowLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),   // left = 내부 세로선
                            style.getBodyOuterBorder(),           // right = 바디 외부 세로선
                            hiddenBorder(),
                            style.getBodyLastRowBottomBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodySingleRowLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),   // left = 내부 세로선
                            style.getBodyOuterBorder(),           // right = 바디 외부 세로선
                            style.getBodyFirstRowTopBorder(),
                            style.getBodyLastRowBottomBorder(),
                            style.getBodyBackgroundColor()
                    );

            RegisteredPair pair = new RegisteredPair();
            pair.headerIds = headerIds;
            pair.bodyIds = bodyIds;

            pair.bodyFirstRowFirstColBorderFillId = bodyFirstRowFirstColBorderFillId;
            pair.bodyMiddleRowFirstColBorderFillId = bodyMiddleRowFirstColBorderFillId;
            pair.bodyLastRowFirstColBorderFillId = bodyLastRowFirstColBorderFillId;
            pair.bodySingleRowFirstColBorderFillId = bodySingleRowFirstColBorderFillId;

            pair.bodyFirstRowOtherColBorderFillId = bodyFirstRowOtherColBorderFillId;
            pair.bodyMiddleRowOtherColBorderFillId = bodyMiddleRowOtherColBorderFillId;
            pair.bodyLastRowOtherColBorderFillId = bodyLastRowOtherColBorderFillId;
            pair.bodySingleRowOtherColBorderFillId = bodySingleRowOtherColBorderFillId;

            pair.bodyFirstRowLastColBorderFillId = bodyFirstRowLastColBorderFillId;
            pair.bodyMiddleRowLastColBorderFillId = bodyMiddleRowLastColBorderFillId;
            pair.bodyLastRowLastColBorderFillId = bodyLastRowLastColBorderFillId;
            pair.bodySingleRowLastColBorderFillId = bodySingleRowLastColBorderFillId;

            pair.headerFontSize = extractFontSize(style.getHeaderTextStyle(), 12);
            pair.bodyFontSize = extractFontSize(style.getBodyTextStyle(), 10);

            result.put(e.getKey(), pair);
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

    private int extractFontSize(ExtractedTableStyle.CellTextStyle textStyle, int defaultValue) {
        if (textStyle == null || textStyle.getFontSize() == null || textStyle.getFontSize() <= 0) {
            return defaultValue;
        }
        return textStyle.getFontSize();
    }

    private byte[] applyTableStyleToAllTablesInSectionXml(
            byte[] sectionXmlBytes,
            RegisteredPair pair
    ) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(sectionXmlBytes)));

        NodeList tblList = doc.getElementsByTagName("hp:tbl");

        for (int i = 0; i < tblList.getLength(); i++) {
            Node node = tblList.item(i);
            if (!(node instanceof Element tblEl)) continue;

            applyStyleToTable(tblEl, pair);
        }

        return documentToBytes(doc);
    }

    private void applyStyleToTable(Element tblEl, RegisteredPair pair) {
        List<Element> rows = getElements(tblEl, "hp:tr");
        int rowCount = rows.size();
        int lastRowIndex = rowCount - 1;

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Element rowEl = rows.get(rowIndex);
            List<Element> cells = getElements(rowEl, "hp:tc");

            boolean isHeaderRow = rowIndex == 0;
            boolean isOnlyOneBodyRow = rowCount == 2 && rowIndex == 1;
            boolean isFirstBodyRow = rowIndex == 1 && rowCount > 2;
            boolean isLastBodyRow = rowIndex == lastRowIndex && rowIndex > 1;
            boolean isMiddleBodyRow = rowIndex > 1 && rowIndex < lastRowIndex;

            int lastColIndex = cells.size() - 1;

            for (int colIndex = 0; colIndex < cells.size(); colIndex++) {
                Element cellEl = cells.get(colIndex);
                boolean isFirstCol = colIndex == 0;
                boolean isLastCol = colIndex == lastColIndex;

                if (isHeaderRow) {
                    applyCellFullStyle(cellEl, pair.headerIds, pair.headerFontSize, null);
                    continue;
                }

                if (isOnlyOneBodyRow) {
                    applyCellFullStyle(
                            cellEl,
                            pair.bodyIds,
                            pair.bodyFontSize,
                            isFirstCol
                                    ? pair.bodySingleRowFirstColBorderFillId
                                    : isLastCol
                                        ? pair.bodySingleRowLastColBorderFillId
                                        : pair.bodySingleRowOtherColBorderFillId
                    );
                    continue;
                }

                if (isFirstBodyRow) {
                    applyCellFullStyle(
                            cellEl,
                            pair.bodyIds,
                            pair.bodyFontSize,
                            isFirstCol
                                    ? pair.bodyFirstRowFirstColBorderFillId
                                    : isLastCol
                                        ? pair.bodyFirstRowLastColBorderFillId
                                        : pair.bodyFirstRowOtherColBorderFillId
                    );
                    continue;
                }

                if (isMiddleBodyRow) {
                    applyCellFullStyle(
                            cellEl,
                            pair.bodyIds,
                            pair.bodyFontSize,
                            isFirstCol
                                    ? pair.bodyMiddleRowFirstColBorderFillId
                                    : isLastCol
                                        ? pair.bodyMiddleRowLastColBorderFillId
                                        : pair.bodyMiddleRowOtherColBorderFillId
                    );
                    continue;
                }

                if (isLastBodyRow) {
                    applyCellFullStyle(
                            cellEl,
                            pair.bodyIds,
                            pair.bodyFontSize,
                            isFirstCol
                                    ? pair.bodyLastRowFirstColBorderFillId
                                    : isLastCol
                                        ? pair.bodyLastRowLastColBorderFillId
                                        : pair.bodyLastRowOtherColBorderFillId
                    );
                }
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

    private byte[] documentToBytes(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    private static class RegisteredPair {
        private HwpxStyleRegistryService.RegisteredStyleIds headerIds;
        private HwpxStyleRegistryService.RegisteredStyleIds bodyIds;

        private String bodyFirstRowFirstColBorderFillId;
        private String bodyMiddleRowFirstColBorderFillId;
        private String bodyLastRowFirstColBorderFillId;
        private String bodySingleRowFirstColBorderFillId;

        private String bodyFirstRowOtherColBorderFillId;
        private String bodyMiddleRowOtherColBorderFillId;
        private String bodyLastRowOtherColBorderFillId;
        private String bodySingleRowOtherColBorderFillId;

        private String bodyFirstRowLastColBorderFillId;
        private String bodyMiddleRowLastColBorderFillId;
        private String bodyLastRowLastColBorderFillId;
        private String bodySingleRowLastColBorderFillId;

        private int headerFontSize;
        private int bodyFontSize;
    }
}