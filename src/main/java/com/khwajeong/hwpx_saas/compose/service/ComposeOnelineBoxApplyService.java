package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.*;
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
public class ComposeOnelineBoxApplyService {

    public Path replaceParagraphWithOnelineBox(
            Path sourceHwpxPath,
            String sourceParagraphId,
            Path templateHwpxPath,
            String templateTableId,
            List<String> slotValues
    ) throws Exception {
        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }
        if (templateHwpxPath == null || !Files.exists(templateHwpxPath)) {
            throw new IllegalArgumentException("template hwpx not found");
        }
        if (sourceParagraphId == null || sourceParagraphId.isBlank()) {
            throw new IllegalArgumentException("sourceParagraphId is required");
        }
        if (templateTableId == null || templateTableId.isBlank()) {
            throw new IllegalArgumentException("templateTableId is required");
        }

        int sourceParagraphIndex = parseParagraphIndex(sourceParagraphId);
        int templateTableIndex = parseTableIndex(templateTableId);

        if (sourceParagraphIndex < 0) {
            throw new IllegalArgumentException("invalid sourceParagraphId: " + sourceParagraphId);
        }
        if (templateTableIndex < 0) {
            throw new IllegalArgumentException("invalid templateTableId: " + templateTableId);
        }

        Path outputPath = Files.createTempFile("compose-onelinebox-replaced-", ".hwpx");

        try (ZipFile sourceZip = new ZipFile(sourceHwpxPath.toFile());
             ZipFile templateZip = new ZipFile(templateHwpxPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {

            Map<String, byte[]> updatedEntries = new LinkedHashMap<>();

            // template에서 oneline_box table element를 먼저 찾는다
            TemplateTableSnapshot templateSnapshot = extractTemplateTableSnapshot(templateZip, templateTableIndex);
            if (templateSnapshot == null || templateSnapshot.tableElement == null) {
                throw new IllegalArgumentException("template table not found: " + templateTableId);
            }

            Enumeration<? extends ZipEntry> entries = sourceZip.entries();
            int[] globalParagraphCounter = new int[]{0};

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    updatedEntries.put(entry.getName(), new byte[0]);
                    continue;
                }

                byte[] bytes;
                try (InputStream in = sourceZip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }

                if (entry.getName().startsWith("Contents/section") && entry.getName().endsWith(".xml")) {
                    bytes = replaceParagraphInSectionXml(
                            bytes,
                            globalParagraphCounter, // 🔥 이거 유지
                            sourceParagraphIndex,
                            templateSnapshot.tableElement,
                            slotValues
                    );
                }

                updatedEntries.put(entry.getName(), bytes);
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

    private TemplateTableSnapshot extractTemplateTableSnapshot(ZipFile templateZip, int templateTableIndex) throws Exception {
        Enumeration<? extends ZipEntry> entries = templateZip.entries();
        int[] tableCounterRef = new int[]{0};

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            if (!entry.getName().startsWith("Contents/section")) continue;
            if (!entry.getName().endsWith(".xml")) continue;

            byte[] bytes;
            try (InputStream in = templateZip.getInputStream(entry)) {
                bytes = in.readAllBytes();
            }

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new ByteArrayInputStream(bytes)));

            NodeList tblList = doc.getElementsByTagName("hp:tbl");
            for (int i = 0; i < tblList.getLength(); i++) {
                Node node = tblList.item(i);
                if (!(node instanceof Element tblEl)) {
                    continue;
                }

                if (tableCounterRef[0] == templateTableIndex) {
                    TemplateTableSnapshot snapshot = new TemplateTableSnapshot();
                    snapshot.tableElement = (Element) tblEl.cloneNode(true);
                    return snapshot;
                }

                tableCounterRef[0]++;
            }
        }

        return null;
    }

    private byte[] replaceParagraphInSectionXml(
            byte[] sectionXmlBytes,
            int[] paragraphCounterRef,
            int targetParagraphIndex,
            Element templateTableElement,
            List<String> slotValues
    ) throws Exception {
        Document sourceDoc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(sectionXmlBytes)));

        NodeList paragraphList = sourceDoc.getElementsByTagName("hp:p");

        for (int i = 0; i < paragraphList.getLength(); i++) {
            Node node = paragraphList.item(i);
            if (!(node instanceof Element paragraphEl)) {
                continue;
            }

            if (paragraphCounterRef[0] == targetParagraphIndex) {
                Node parent = paragraphEl.getParentNode();
                if (parent == null) {
                    throw new IllegalArgumentException("source paragraph parent not found");
                }

                Element importedTable = (Element) sourceDoc.importNode(templateTableElement, true);
                injectSlotValuesIntoTable(importedTable, slotValues);

                parent.replaceChild(importedTable, paragraphEl);
                return documentToBytes(sourceDoc);
            }

            paragraphCounterRef[0]++;
        }

        return documentToBytes(sourceDoc);
    }

    private void injectSlotValuesIntoTable(Element tblEl, List<String> slotValues) {
        if (tblEl == null || slotValues == null) {
            return;
        }

        List<Element> rows = getDirectChildren(tblEl, "hp:tr");
        if (rows.isEmpty()) {
            return;
        }

        Element firstRow = rows.get(0);
        List<Element> cells = getDirectChildren(firstRow, "hp:tc");

        for (int i = 0; i < cells.size(); i++) {
            String value = i < slotValues.size() ? slotValues.get(i) : "";
            setCellText(cells.get(i), value);
        }
    }

    private void setCellText(Element cellEl, String text) {
        if (cellEl == null) {
            return;
        }

        String normalized = text == null ? "" : text;

        List<Element> paraList = getElements(cellEl, "hp:p");
        if (paraList.isEmpty()) {
            return;
        }

        boolean firstApplied = false;

        for (Element paraEl : paraList) {
            NodeList textNodes = paraEl.getElementsByTagName("hp:t");

            if (textNodes.getLength() == 0 && !firstApplied) {
                Element runEl = findOrCreateDirectChild(paraEl, "hp:run");
                Element textEl = paraEl.getOwnerDocument().createElement("hp:t");
                textEl.setTextContent(normalized);
                runEl.appendChild(textEl);
                firstApplied = true;
                continue;
            }

            for (int i = 0; i < textNodes.getLength(); i++) {
                Node node = textNodes.item(i);
                if (!(node instanceof Element textEl)) {
                    continue;
                }

                if (!firstApplied) {
                    textEl.setTextContent(normalized);
                    firstApplied = true;
                } else {
                    textEl.setTextContent("");
                }
            }
        }
    }

    private Element findOrCreateDirectChild(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element el) {
                String nodeName = el.getNodeName();
                String localName = el.getLocalName();
                if (tagName.equalsIgnoreCase(nodeName) || tagName.equalsIgnoreCase(localName)) {
                    return el;
                }
            }
        }

        Element created = parent.getOwnerDocument().createElement(tagName);
        parent.appendChild(created);
        return created;
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

    private int parseParagraphIndex(String paragraphId) {
        if (paragraphId == null || paragraphId.isBlank() || !paragraphId.startsWith("p")) {
            return -1;
        }
        try {
            return Integer.parseInt(paragraphId.substring(1));
        } catch (Exception e) {
            return -1;
        }
    }

    private int parseTableIndex(String tableId) {
        if (tableId == null || tableId.isBlank() || !tableId.startsWith("t")) {
            return -1;
        }
        try {
            return Integer.parseInt(tableId.substring(1));
        } catch (Exception e) {
            return -1;
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

    private static class TemplateTableSnapshot {
        private Element tableElement;
    }
}