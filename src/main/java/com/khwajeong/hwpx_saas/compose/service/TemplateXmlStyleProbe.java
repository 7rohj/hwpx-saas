package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class TemplateXmlStyleProbe {

    public static class ParagraphSample {
        private final String text;
        private final String paraPrId;
        private final String charPrId;

        public ParagraphSample(String text, String paraPrId, String charPrId) {
            this.text = text;
            this.paraPrId = paraPrId;
            this.charPrId = charPrId;
        }

        public String getText() {
            return text;
        }

        public String getParaPrId() {
            return paraPrId;
        }

        public String getCharPrId() {
            return charPrId;
        }

        @Override
        public String toString() {
            return "ParagraphSample{" +
                    "text='" + text + '\'' +
                    ", paraPrId='" + paraPrId + '\'' +
                    ", charPrId='" + charPrId + '\'' +
                    '}';
        }
    }

    public List<ParagraphSample> extractParagraphSamples(Path hwpxPath) throws Exception {
        List<ParagraphSample> result = new ArrayList<>();

        try (ZipFile zip = new ZipFile(hwpxPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!isCandidateXml(name)) {
                    continue;
                }

                try (InputStream is = zip.getInputStream(entry)) {
                    String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    xml = stripBom(xml);

                    Document doc = DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(new InputSource(new StringReader(xml)));

                    Element root = doc.getDocumentElement();
                    if (root == null) continue;

                    traverse(root, result);
                }
            }
        }

        return result;
    }

    private void traverse(Node node, List<ParagraphSample> out) {
        if (node instanceof Element element) {
            if (isParagraphElement(element)) {
                String paraPrId = attr(element, "paraPrIDRef");
                String charPrId = findFirstCharPrId(element);
                String text = collectParagraphText(element).trim();

                if (!text.isBlank()) {
                    out.add(new ParagraphSample(text, paraPrId, charPrId));
                }
            }
        }

        Node child = node.getFirstChild();
        while (child != null) {
            traverse(child, out);
            child = child.getNextSibling();
        }
    }

    private boolean isCandidateXml(String name) {
        if (name == null || !name.endsWith(".xml")) return false;
        return name.startsWith("Contents/section")
                || name.startsWith("Contents/header")
                || name.startsWith("Contents/footer")
                || name.startsWith("Contents/master");
    }

    private boolean isParagraphElement(Element e) {
        String nodeName = stripPrefix(e.getNodeName()).toLowerCase();
        return "p".equals(nodeName) || e.hasAttribute("paraPrIDRef");
    }

    private String findFirstCharPrId(Element root) {
        if (root.hasAttribute("charPrIDRef")) {
            return root.getAttribute("charPrIDRef");
        }

        NodeList all = root.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (n instanceof Element e && e.hasAttribute("charPrIDRef")) {
                return e.getAttribute("charPrIDRef");
            }
        }
        return null;
    }

    private String collectParagraphText(Element root) {
        StringBuilder sb = new StringBuilder();

        NodeList all = root.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (!(n instanceof Element e)) continue;

            String local = stripPrefix(e.getNodeName()).toLowerCase();
            if ("t".equals(local)) {
                String text = e.getTextContent();
                if (text != null && !text.isBlank()) {
                    if (!sb.isEmpty()) sb.append(" ");
                    sb.append(text.trim());
                }
            }
        }

        return sb.toString();
    }

    private String attr(Element e, String attrName) {
        return e.hasAttribute(attrName) ? e.getAttribute(attrName) : null;
    }

    private String stripPrefix(String name) {
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    private String stripBom(String s) {
        if (s == null || s.isEmpty()) return s;
        if (s.charAt(0) == '\uFEFF') return s.substring(1);
        return s;
    }
}