package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class HwpxParagraphExtractor {

    public List<SimpleParagraph> extract(File hwpxFile) {
        List<SimpleParagraph> result = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(hwpxFile)) {
            List<? extends ZipEntry> sectionEntries = findSectionEntries(zipFile);

            if (sectionEntries.isEmpty()) {
                throw new RuntimeException("no section xml found in hwpx");
            }

            int globalIndex = 0;

            for (ZipEntry entry : sectionEntries) {
                try (InputStream in = zipFile.getInputStream(entry)) {
                    Document doc = DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(in);

                    NodeList paraList = doc.getElementsByTagName("hp:p");

                    for (int i = 0; i < paraList.getLength(); i++) {
                        Element p = (Element) paraList.item(i);

                        String text = extractText(p);
                        if (text == null || text.trim().isEmpty()) {
                            continue;
                        }

                        SimpleParagraph sp = new SimpleParagraph();
                        sp.setParagraphId("p" + globalIndex++);
                        sp.setText(text.trim());
                        sp.setSingleLine(!text.contains("\n"));

                        // 수정 없음: 문단 스타일 참조 ID 추출
                        sp.setParaPrIdRef(readAttrIgnoreCase(p, "paraPrIDRef"));

                        // 수정 없음: 첫 run의 문자 스타일 참조 ID 추출
                        String charPrIdRef = extractFirstCharPrIdRef(p);
                        sp.setCharPrIdRef(charPrIdRef);

                        result.add(sp);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse hwpx", e);
        }

        return result;
    }

    private List<? extends ZipEntry> findSectionEntries(ZipFile zipFile) {
        List<ZipEntry> sections = new ArrayList<>();

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry e = entries.nextElement();
            String name = e.getName();

            if (name == null) {
                continue;
            }

            if (name.startsWith("Contents/section") && name.endsWith(".xml")) {
                sections.add(e);
            }
        }

        sections.sort(Comparator.comparing(ZipEntry::getName));
        return sections;
    }

    private String extractText(Element p) {
        NodeList runs = p.getElementsByTagName("hp:t");

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < runs.getLength(); i++) {
            Node n = runs.item(i);
            sb.append(n.getTextContent());
        }

        return sb.toString();
    }

    private String extractFirstCharPrIdRef(Element p) {
        NodeList runs = p.getElementsByTagName("hp:run");

        for (int i = 0; i < runs.getLength(); i++) {
            Node node = runs.item(i);
            if (node instanceof Element runEl) {
                String value = readAttrIgnoreCase(runEl, "charPrIDRef");
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }

        return null;
    }

    private String readAttrIgnoreCase(Element element, String attrName) {
        if (element == null || attrName == null) {
            return null;
        }

        NamedNodeMap attrs = element.getAttributes();
        if (attrs == null) {
            return null;
        }

        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (attr == null) {
                continue;
            }

            String nodeName = attr.getNodeName();
            String localName = attr.getLocalName();

            if (attrName.equalsIgnoreCase(nodeName) || attrName.equalsIgnoreCase(localName)) {
                return attr.getNodeValue();
            }
        }

        return null;
    }

    public static class SimpleParagraph {
        private String paragraphId;
        private String text;
        private boolean singleLine;

        private String paraPrIdRef;
        private String charPrIdRef;

        public String getParagraphId() {
            return paragraphId;
        }

        public void setParagraphId(String paragraphId) {
            this.paragraphId = paragraphId;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public boolean isSingleLine() {
            return singleLine;
        }

        public void setSingleLine(boolean singleLine) {
            this.singleLine = singleLine;
        }

        public String getParaPrIdRef() {
            return paraPrIdRef;
        }

        public void setParaPrIdRef(String paraPrIdRef) {
            this.paraPrIdRef = paraPrIdRef;
        }

        public String getCharPrIdRef() {
            return charPrIdRef;
        }

        public void setCharPrIdRef(String charPrIdRef) {
            this.charPrIdRef = charPrIdRef;
        }
    }
}