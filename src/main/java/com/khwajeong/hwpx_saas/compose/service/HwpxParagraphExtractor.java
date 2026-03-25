package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class HwpxParagraphExtractor {

    public List<SimpleParagraph> extract(File hwpxFile) {
        List<SimpleParagraph> result = new ArrayList<>();

        try {
            // hwpx = zip → content.xml 읽기
            java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(hwpxFile);
            java.util.zip.ZipEntry entry = zipFile.getEntry("Contents/section0.xml");

            if (entry == null) {
                throw new RuntimeException("section0.xml not found");
            }

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(zipFile.getInputStream(entry));

            NodeList paraList = doc.getElementsByTagName("hp:p");

            for (int i = 0; i < paraList.getLength(); i++) {
                Element p = (Element) paraList.item(i);

                String text = extractText(p);

                if (text == null || text.trim().isEmpty()) continue;

                SimpleParagraph sp = new SimpleParagraph();
                sp.setParagraphId("p" + i);
                sp.setText(text.trim());
                sp.setSingleLine(!text.contains("\n"));

                result.add(sp);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse hwpx", e);
        }

        return result;
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

    public static class SimpleParagraph {
        private String paragraphId;
        private String text;
        private boolean singleLine;

        public String getParagraphId() { return paragraphId; }
        public void setParagraphId(String paragraphId) { this.paragraphId = paragraphId; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public boolean isSingleLine() { return singleLine; }
        public void setSingleLine(boolean singleLine) { this.singleLine = singleLine; }
    }
}