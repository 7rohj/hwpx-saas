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

                        // 1) 표 내부 문단 제거
                        if (isInsideTable(p)) {
                            continue;
                        }

                        // 2) 표를 품고 있는 바깥 문단 제거
                        //    (이 문단의 하위 hp:tbl 안 텍스트가 제목1제목2... 로 중복 추출되는 문제 방지)
                        if (containsTableDescendant(p)) {
                            continue;
                        }

                        String text = extractText(p);
                        if (text == null || text.trim().isEmpty()) {
                            continue;
                        }

                        SimpleParagraph sp = new SimpleParagraph();
                        sp.setParagraphId("p" + globalIndex++);
                        sp.setText(text.trim());
                        sp.setSingleLine(!text.contains("\n"));

                        sp.setParaPrIdRef(readAttrIgnoreCase(p, "paraPrIDRef"));

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

    /**
     * 현재 문단이 표 내부에 들어있는지 확인
     */
    private boolean isInsideTable(Node node) {
        Node parent = node.getParentNode();

        while (parent != null) {
            if (parent instanceof Element el) {
                String name = el.getNodeName();
                String local = el.getLocalName();

                if ("hp:tbl".equalsIgnoreCase(name) || "tbl".equalsIgnoreCase(local)) {
                    return true;
                }
            }
            parent = parent.getParentNode();
        }

        return false;
    }

    /**
     * 현재 문단이 자기 하위에 표를 포함하고 있는지 확인
     * 예: 바깥 hp:p 안에 hp:tbl 이 들어있는 경우
     */
    private boolean containsTableDescendant(Element p) {
        if (p == null) {
            return false;
        }

        NodeList tblList = p.getElementsByTagName("hp:tbl");
        if (tblList != null && tblList.getLength() > 0) {
            return true;
        }

        // namespace/localName 대비용
        NodeList childNodes = p.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (!(node instanceof Element el)) {
                continue;
            }

            String name = el.getNodeName();
            String local = el.getLocalName();

            if ("hp:tbl".equalsIgnoreCase(name) || "tbl".equalsIgnoreCase(local)) {
                return true;
            }
        }

        return false;
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

    /**
     * 일반 문단 텍스트만 추출
     * 현재는 표를 포함한 문단은 위에서 skip하므로,
     * 여기서는 hp:t를 그대로 이어붙여도 안전
     */
    private String extractText(Element p) {
        NodeList runs = p.getElementsByTagName("hp:t");

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < runs.getLength(); i++) {
            Node n = runs.item(i);
            if (n == null) {
                continue;
            }
            String value = n.getTextContent();
            if (value != null) {
                sb.append(value);
            }
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