package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ExtractedStyle;
import com.khwajeong.hwpx_saas.compose.model.TemplateClonePlan;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleSet;
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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class TemplateCloneRenderer {

    public Path render(
            Path templatePath,
            TemplateClonePlan plan,
            TemplateStyleSet styleSet
    ) throws Exception {

        Files.createDirectories(Path.of("generated-compose"));

        Path output = Path.of("generated-compose", "cloned_" + System.currentTimeMillis() + ".hwpx");

        try (ZipFile zipFile = new ZipFile(templatePath.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output))) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                zos.putNextEntry(new ZipEntry(name));

                if ("Contents/section0.xml".equals(name)) {
                    String xml = new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                    xml = injectParagraphs(xml, plan, styleSet);
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else {
                    zos.write(zipFile.getInputStream(entry).readAllBytes());
                }

                zos.closeEntry();
            }
        }

        return output;
    }

    private String injectParagraphs(
            String xml,
            TemplateClonePlan plan,
            TemplateStyleSet styleSet
    ) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        Element root = doc.getDocumentElement();
        if (root == null) return xml;

        Element sectionRoot = findSectionRoot(root);
        if (sectionRoot == null) return xml;

        for (TemplateClonePlan.CloneBlock block : plan.getBlocks()) {
            ExtractedStyle style = styleSet.get(block.getRoleType());
            if (style == null || style.getParagraphXml() == null || style.getParagraphXml().isBlank()) {
                continue;
            }

            Node importedPara = buildParagraphFromSample(doc, style.getParagraphXml(), block.getText());
            sectionRoot.appendChild(importedPara);
        }

        return toXml(doc);
    }

    private Element findSectionRoot(Element root) {
        return root;
    }

    private Node buildParagraphFromSample(Document targetDoc, String paragraphXml, String newText) throws Exception {
        Document paraDoc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(paragraphXml)));

        Node paraNode = paraDoc.getDocumentElement();
        replaceAllTextNodes(paraNode, newText);

        return targetDoc.importNode(paraNode, true);
    }

    private void replaceAllTextNodes(Node node, String newText) {
        if (node instanceof Element e) {
            String localName = stripPrefix(e.getNodeName()).toLowerCase();
            if ("t".equals(localName)) {
                e.setTextContent(newText);
            }
        }

        Node child = node.getFirstChild();
        while (child != null) {
            replaceAllTextNodes(child, newText);
            child = child.getNextSibling();
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