package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ExtractedTableStyle;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;

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

            Map<String, byte[]> updatedEntries = new HashMap<>();

            // 1) 먼저 header xml 들을 로딩해서 style 등록
            Map<String, Document> headerDocs = loadHeaderDocs(zipFile);
            Map<String, RegisteredPair> registeredByHeader = registerStylesToHeaders(headerDocs, style);

            // 2) section xml 수정
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            int sectionIndex = 0;

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
                    String sectionName = entry.getName();
                    String headerName = toHeaderName(sectionName);
                    RegisteredPair pair = registeredByHeader.get(headerName);

                    if (pair != null) {
                        bytes = applyTableStyleToSectionXml(bytes, sourceTableId, style, pair);
                    }
                }

                updatedEntries.put(entry.getName(), bytes);
                sectionIndex++;
            }

            // 3) 업데이트된 header xml 반영
            for (Map.Entry<String, Document> e : headerDocs.entrySet()) {
                updatedEntries.put(e.getKey(), documentToBytes(e.getValue()));
            }

            // 4) zip 재작성
            for (Map.Entry<String, byte[]> e : updatedEntries.entrySet()) {
                String name = e.getKey();
                byte[] bytes = e.getValue();

                ZipEntry newEntry = new ZipEntry(name);
                zos.putNextEntry(newEntry);

                if (!name.endsWith("/") && bytes != null && bytes.length > 0) {
                    zos.write(bytes);
                }

                zos.closeEntry();
            }
        }

        return outputPath;
    }

    private Map<String, Document> loadHeaderDocs(ZipFile zipFile) throws Exception {
        Map<String, Document> result = new HashMap<>();

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
        Map<String, RegisteredPair> result = new HashMap<>();

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
                            style.getBodyOuterBorder(),
                            style.getBodyInnerBorder(),
                            style.getBodyBackgroundColor()
                    );

            RegisteredPair pair = new RegisteredPair();
            pair.headerIds = headerIds;
            pair.bodyIds = bodyIds;

            result.put(e.getKey(), pair);
        }

        return result;
    }

    private byte[] applyTableStyleToSectionXml(
            byte[] sectionXmlBytes,
            String sourceTableId,
            ExtractedTableStyle style,
            RegisteredPair registeredPair
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

            applyStyleToTable(tblEl, registeredPair);
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
                HwpxStyleRegistryService.RegisteredStyleIds ids =
                        isHeaderRow ? pair.headerIds : pair.bodyIds;

                applyCellRefs(cellEl, ids);
            }
        }
    }

    private void applyCellRefs(Element cellEl, HwpxStyleRegistryService.RegisteredStyleIds ids) {
        if (cellEl == null || ids == null) return;

        if (ids.getBorderFillId() != null) {
            cellEl.setAttribute("borderFillIDRef", ids.getBorderFillId());
        }

        List<Element> paraList = getElements(cellEl, "hp:p");
        for (Element paraEl : paraList) {
            if (ids.getParaPrId() != null) {
                paraEl.setAttribute("paraPrIDRef", ids.getParaPrId());
            }

            List<Element> runList = getElements(paraEl, "hp:run");
            for (Element runEl : runList) {
                if (ids.getCharPrId() != null) {
                    runEl.setAttribute("charPrIDRef", ids.getCharPrId());
                }
            }
        }
    }

    private String toHeaderName(String sectionName) {
        String headerName = sectionName
                .replace("Contents/section", "Contents/header");

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