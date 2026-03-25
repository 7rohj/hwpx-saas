package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParagraphFormatProfile;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class SourceDocumentStyleApplier {

    public Path apply(
            Path sourcePath,
            List<StyleMapping> mappings,
            Map<TemplateRoleType, ParagraphFormatProfile> profiles
    ) throws Exception {

        Files.createDirectories(Path.of("generated-compose"));
        Path output = Path.of("generated-compose", "styled_" + System.currentTimeMillis() + ".hwpx");

        try (ZipFile zipFile = new ZipFile(sourcePath.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output))) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                zos.putNextEntry(new ZipEntry(name));

                if ("Contents/section0.xml".equals(name)) {
                    String xml = new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                    String updated = applyStylesToSection(xml, mappings, profiles);
                    zos.write(updated.getBytes(StandardCharsets.UTF_8));
                } else {
                    zos.write(zipFile.getInputStream(entry).readAllBytes());
                }

                zos.closeEntry();
            }
        }

        return output;
    }

    private String applyStylesToSection(
            String xml,
            List<StyleMapping> mappings,
            Map<TemplateRoleType, ParagraphFormatProfile> profiles
    ) throws Exception {

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        List<Element> paragraphs = collectParagraphElements(doc.getDocumentElement());

        // 1차: source analyzer의 순서와 실제 문단 순서가 크게 다르지 않다고 가정
        int limit = Math.min(paragraphs.size(), mappings.size());

        for (int i = 0; i < limit; i++) {
            Element para = paragraphs.get(i);
            StyleMapping mapping = mappings.get(i);

            if (mapping.getTargetRole() == TemplateRoleType.TABLE_HEADER
                    || mapping.getTargetRole() == TemplateRoleType.TABLE_BODY) {
                continue;
            }

            ParagraphFormatProfile profile = profiles.get(mapping.getTargetRole());
            if (profile == null) continue;

            applyParagraphFormat(para, profile);
        }

        return toXml(doc);
    }

    private List<Element> collectParagraphElements(Element root) {
        List<Element> result = new ArrayList<>();
        traverse(root, result);
        return result;
    }

    private void traverse(Node node, List<Element> out) {
        if (node instanceof Element e) {
            String local = stripPrefix(e.getNodeName()).toLowerCase();
            if ("p".equals(local) || e.hasAttribute("paraPrIDRef")) {
                out.add(e);
            }
        }

        Node child = node.getFirstChild();
        while (child != null) {
            traverse(child, out);
            child = child.getNextSibling();
        }
    }

    private void applyParagraphFormat(Element para, ParagraphFormatProfile profile) {
        if (profile.getParaPrId() != null && !profile.getParaPrId().isBlank()) {
            para.setAttribute("paraPrIDRef", profile.getParaPrId());
        }

        if (profile.getCharPrId() != null && !profile.getCharPrId().isBlank()) {
            applyCharPrToDescendants(para, profile.getCharPrId());
        }
    }

    private void applyCharPrToDescendants(Element para, String charPrId) {
        NodeList all = para.getElementsByTagName("*");

        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (!(n instanceof Element e)) continue;

            if (e.hasAttribute("charPrIDRef")) {
                e.setAttribute("charPrIDRef", charPrId);
            }
        }
    }

    private String stripPrefix(String name) {
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    private String toXml(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}