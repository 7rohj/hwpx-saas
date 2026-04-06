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
        if (sourceTableId == null || sourceTableId.isBlank()) {
            throw new IllegalArgumentException("sourceTableId is required");
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
                        bytes = applyTableStyleToSectionXml(bytes, sourceTableId, pair);
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

            // 헤더 스타일 등록:
            // - backgroundColor
            // - borderFill
            // - header text용 charPr / paraPr
            HwpxStyleRegistryService.RegisteredStyleIds headerIds =
                    hwpxStyleRegistryService.registerTableCellStyle(
                            doc,
                            style.getHeaderTextStyle(),
                            style.getHeaderOuterBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderBackgroundColor()
                    );

            RegisteredPair pair = new RegisteredPair();
            pair.headerIds = headerIds;
            pair.bodyIds = null;

            result.put(e.getKey(), pair);
        }

        return result;
    }

    private byte[] applyTableStyleToSectionXml(
            byte[] sectionXmlBytes,
            String sourceTableId,
            RegisteredPair pair
    ) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(sectionXmlBytes)));

        NodeList tblList = doc.getElementsByTagName("hp:tbl");
        int tableIndex = 0;

        for (int i = 0; i < tblList.getLength(); i++) {
            Node node = tblList.item(i);
            if (!(node instanceof Element tblEl)) continue;

            String currentTableId = "t" + tableIndex;
            tableIndex++;

            if (!sourceTableId.equals(currentTableId)) {
                continue;
            }

            applyStyleToTable(tblEl, pair);
        }

        return documentToBytes(doc);
    }

    private void applyStyleToTable(Element tblEl, RegisteredPair pair) {
        List<Element> rows = getElements(tblEl, "hp:tr");

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Element rowEl = rows.get(rowIndex);
            List<Element> cells = getElements(rowEl, "hp:tc");

            boolean isHeaderRow = rowIndex == 0;

            for (Element cellEl : cells) {
                if (isHeaderRow) {
                    // 헤더 row 0에만:
                    // 1) 셀 background/border borderFillIDRef
                    // 2) 내부 p/run의 paraPrIDRef / charPrIDRef
                    applyHeaderCellFullStyle(cellEl, pair.headerIds);
                }
                // 바디는 지금 단계에서 건드리지 않음
            }
        }
    }

    private void applyHeaderCellFullStyle(
            Element cellEl,
            HwpxStyleRegistryService.RegisteredStyleIds ids
    ) {
        if (cellEl == null || ids == null) {
            return;
        }

        // 1) 셀 자체 배경/선 스타일 적용
        if (ids.getBorderFillId() != null && !ids.getBorderFillId().isBlank()) {
            cellEl.setAttribute("borderFillIDRef", ids.getBorderFillId());
        }

        // 2) 헤더 셀 내부 문단/텍스트 스타일 적용
        // template_section0.xml 기준으로 header cell 내부는 hp:subList > hp:p > hp:run 구조
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
        }
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
    }
}