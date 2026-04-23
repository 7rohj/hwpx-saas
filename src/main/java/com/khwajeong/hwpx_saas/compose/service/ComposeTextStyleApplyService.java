package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import io.github.mumberrymountain.model.Text;
import io.github.mumberrymountain.model.table.Align;
import io.github.mumberrymountain.render.style.StyleRenderer;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class ComposeTextStyleApplyService {

    public Path applyTextStyleUsingRenderer(
            Path sourceHwpxPath,
            StyleProfileDto templateStyle,
            List<ParagraphNodeDto> sourceParagraphs,
            List<String> matchedParagraphIds,
            String prefixToInject
    ) throws Exception {

        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }
        if (templateStyle == null) {
            throw new IllegalArgumentException("templateStyle is required");
        }
        if (sourceParagraphs == null || sourceParagraphs.isEmpty()) {
            throw new IllegalArgumentException("sourceParagraphs is empty");
        }
        if (matchedParagraphIds == null || matchedParagraphIds.isEmpty()) {
            throw new IllegalArgumentException("matchedParagraphIds is empty");
        }

        HWPXFile hwpxFile = HWPXReader.fromFilepath(sourceHwpxPath.toString());

        StyleRenderer styleRenderer = new StyleRenderer(hwpxFile.headerXMLFile());

        Text textStyle = new Text("")
                .fontFamily(nvl(templateStyle.getFontFamily(), "맑은 고딕"))
                .fontSize(nvl(templateStyle.getFontSize(), 12))
                .bold(Boolean.TRUE.equals(templateStyle.getBold()))
                .italic(Boolean.TRUE.equals(templateStyle.getItalic()))
                .underLine(Boolean.TRUE.equals(templateStyle.getUnderline()))
                .fontColor(nvl(templateStyle.getColor(), "#000000"))
                .align(toRendererAlignString(templateStyle.getAlign()))
                .lineSpacingPercent(templateStyle.getLineSpacing())
                .spaceBefore(templateStyle.getSpaceBefore())
                .spaceAfter(templateStyle.getSpaceAfter());

        String newCharPrId = styleRenderer.renderCharStyleAndReturnCharPrId(textStyle);

        // p7 같은 값을 sourceParagraphs 기준 인덱스로 해석
        Set<Integer> targetStylableIndexes = parseParagraphIndexes(matchedParagraphIds);

        Map<Integer, String> stylableIndexToNewParaPrId = new HashMap<>();
        for (int idx : targetStylableIndexes) {
            if (idx < 0 || idx >= sourceParagraphs.size()) {
                continue;
            }

            ParagraphNodeDto p = sourceParagraphs.get(idx);
            String baseParaPrId = p.getParaPrIdRef();
            Align alignEnum = toAlignEnum(templateStyle.getAlign());

            String newParaPrId = styleRenderer.renderParaStyleFromBaseAndReturnParaPrId(
                    baseParaPrId,
                    alignEnum,
                    templateStyle.getLineSpacing(),
                    templateStyle.getSpaceBefore(),
                    templateStyle.getSpaceAfter()
            );

            stylableIndexToNewParaPrId.put(idx, newParaPrId);
        }

        Path tempStyledSource = Files.createTempFile("compose-style-base-", ".hwpx");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            HWPXWriter.toStream(hwpxFile, baos);
            Files.write(tempStyledSource, baos.toByteArray());
        }

        Path outputPath = Files.createTempFile("compose-render-output-", ".hwpx");

        int globalStylableParagraphIndex = 0;

        try (
                ZipFile sourceZip = new ZipFile(tempStyledSource.toFile());
                ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))
        ) {
            Map<String, byte[]> rewrittenEntries = new LinkedHashMap<>();

            Enumeration<? extends ZipEntry> entries = sourceZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                byte[] originalBytes;
                try (InputStream in = sourceZip.getInputStream(entry)) {
                    originalBytes = in.readAllBytes();
                }

                byte[] outputBytes = originalBytes;

                if (entryName.startsWith("Contents/section") && entryName.endsWith(".xml")) {
                    SectionRewriteResult result = rewriteSectionXml(
                            originalBytes,
                            globalStylableParagraphIndex,
                            targetStylableIndexes,
                            stylableIndexToNewParaPrId,
                            newCharPrId,
                            templateStyle.getFontSize(),
                            prefixToInject
                    );
                    outputBytes = result.bytes;
                    globalStylableParagraphIndex += result.processedStylableParagraphCount;
                }

                rewrittenEntries.put(entryName, outputBytes);
            }

            for (Map.Entry<String, byte[]> e : rewrittenEntries.entrySet()) {
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

    private Set<Integer> parseParagraphIndexes(List<String> matchedParagraphIds) {
        Set<Integer> targetIndexes = new HashSet<>();

        for (String id : matchedParagraphIds) {
            if (id == null) continue;

            String s = id.trim();
            if (s.startsWith("p") || s.startsWith("P")) {
                s = s.substring(1);
            }

            try {
                targetIndexes.add(Integer.parseInt(s));
            } catch (Exception ignored) {
            }
        }

        return targetIndexes;
    }

    private SectionRewriteResult rewriteSectionXml(
            byte[] xmlBytes,
            int globalStylableStartIndex,
            Set<Integer> targetStylableIndexes,
            Map<Integer, String> stylableIndexToNewParaPrId,
            String newCharPrId,
            Integer templateFontSize,
            String prefixToInject
    ) throws Exception {

        Document doc = parseXml(xmlBytes);
        NodeList paraList = doc.getElementsByTagName("hp:p");

        int stylableLocalCounter = 0;

        for (int i = 0; i < paraList.getLength(); i++) {
            Element p = (Element) paraList.item(i);

            if (!isStylableParagraph(p)) {
                continue;
            }

            int globalStylableIndex = globalStylableStartIndex + stylableLocalCounter;
            stylableLocalCounter++;

            if (!targetStylableIndexes.contains(globalStylableIndex)) {
                continue;
            }

            String newParaPrId = stylableIndexToNewParaPrId.get(globalStylableIndex);
            if (newParaPrId != null && !newParaPrId.isBlank()) {
                p.setAttribute("paraPrIDRef", newParaPrId);
            }

            if (prefixToInject != null && !prefixToInject.isBlank()) {
                injectPrefixIntoParagraph(p, prefixToInject);
            }

            NodeList childNodes = p.getChildNodes();
            for (int r = 0; r < childNodes.getLength(); r++) {
                Node child = childNodes.item(r);

                if (!(child instanceof Element runEl)) {
                    continue;
                }

                String nodeName = runEl.getNodeName();
                String localName = runEl.getLocalName();

                boolean isRun =
                        "hp:run".equalsIgnoreCase(nodeName) ||
                        "run".equalsIgnoreCase(localName);

                if (!isRun) {
                    continue;
                }

                if (hasTextDescendant(runEl)) {
                    runEl.setAttribute("charPrIDRef", newCharPrId);
                }
            }

            for (int r = 0; r < childNodes.getLength(); r++) {
                Node child = childNodes.item(r);

                if (!(child instanceof Element rEl)) {
                    continue;
                }

                String nodeName = rEl.getNodeName();
                String localName = rEl.getLocalName();

                boolean isR =
                        "hp:r".equalsIgnoreCase(nodeName) ||
                        "r".equalsIgnoreCase(localName);

                if (!isR) {
                    continue;
                }

                if (hasTextDescendant(rEl)) {
                    rEl.setAttribute("charPrIDRef", newCharPrId);
                }
            }

            p.removeAttribute("styleIDRef");
        }

        recomputeLineSegLayout(
                doc,
                globalStylableStartIndex,
                targetStylableIndexes,
                templateFontSize
        );

        SectionRewriteResult result = new SectionRewriteResult();
        result.bytes = toBytes(doc);
        result.processedStylableParagraphCount = stylableLocalCounter;
        return result;
    }

    private boolean isStylableParagraph(Element paragraphElement) {
        if (paragraphElement == null) {
            return false;
        }

        if (isInsideTable(paragraphElement)) {
            return false;
        }

        if (isTableWrapperParagraph(paragraphElement)) {
            return false;
        }

        return hasDirectVisibleText(paragraphElement);
    }

    private boolean isTableWrapperParagraph(Element paragraphElement) {
        if (paragraphElement == null) {
            return false;
        }

        NodeList tblList = paragraphElement.getElementsByTagName("hp:tbl");
        if (tblList != null && tblList.getLength() > 0) {
            return true;
        }

        NodeList genericTblList = paragraphElement.getElementsByTagName("tbl");
        return genericTblList != null && genericTblList.getLength() > 0;
    }

    private boolean hasDirectVisibleText(Element paragraphElement) {
        if (paragraphElement == null) {
            return false;
        }

        NodeList childNodes = paragraphElement.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (!(child instanceof Element el)) {
                continue;
            }

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            boolean isRun =
                    "hp:run".equalsIgnoreCase(nodeName) ||
                    "run".equalsIgnoreCase(localName) ||
                    "hp:r".equalsIgnoreCase(nodeName) ||
                    "r".equalsIgnoreCase(localName);

            if (!isRun) {
                continue;
            }

            if (hasTextDescendant(el)) {
                return true;
            }
        }

        return false;
    }

    private void injectPrefixIntoParagraph(Element paragraphElement, String prefixToInject) {
        if (paragraphElement == null || prefixToInject == null || prefixToInject.isBlank()) {
            return;
        }

        Element firstTextEl = findFirstTextElement(paragraphElement);
        if (firstTextEl == null) {
            return;
        }

        String original = firstTextEl.getTextContent();
        if (original == null) {
            original = "";
        }

        String withoutExistingPrefix = stripKnownLeadingPrefix(original);
        String finalText = prefixToInject + withoutExistingPrefix.stripLeading();

        firstTextEl.setTextContent(finalText);
    }

    private String stripKnownLeadingPrefix(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text.replaceFirst("^\\s*[■□●○•※▶▷◆◇☞\\-–—]\\s*", "");
    }

    private Element findFirstTextElement(Element paragraphElement) {
        if (paragraphElement == null) {
            return null;
        }

        NodeList tList = paragraphElement.getElementsByTagName("hp:t");
        for (int i = 0; i < tList.getLength(); i++) {
            Node node = tList.item(i);
            if (node instanceof Element el) {
                return el;
            }
        }
        return null;
    }

    private boolean hasTextDescendant(Element element) {
        if (element == null) {
            return false;
        }

        NodeList tList = element.getElementsByTagName("hp:t");
        for (int i = 0; i < tList.getLength(); i++) {
            Node tNode = tList.item(i);
            if (tNode != null) {
                String text = tNode.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isInsideTable(Node node) {
        Node parent = node != null ? node.getParentNode() : null;

        while (parent != null) {
            if (parent instanceof Element el) {
                String nodeName = el.getNodeName();
                String localName = el.getLocalName();

                boolean isTable =
                        "hp:tbl".equalsIgnoreCase(nodeName) ||
                        "tbl".equalsIgnoreCase(localName);

                if (isTable) {
                    return true;
                }
            }
            parent = parent.getParentNode();
        }

        return false;
    }

    private void recomputeLineSegLayout(
            Document doc,
            int globalStylableStartIndex,
            Set<Integer> targetStylableIndexes,
            Integer templateFontSize
    ) {
        NodeList paraList = doc.getElementsByTagName("hp:p");

        int currentVertPos = 0;
        int stylableLocalCounter = 0;

        for (int i = 0; i < paraList.getLength(); i++) {
            Element p = (Element) paraList.item(i);

            if (!isStylableParagraph(p)) {
                continue;
            }

            int globalStylableIndex = globalStylableStartIndex + stylableLocalCounter;
            stylableLocalCounter++;

            Element lineSeg = findFirstLineSegElement(p);
            if (lineSeg == null) {
                continue;
            }

            int textHeight;
            if (targetStylableIndexes.contains(globalStylableIndex)
                    && templateFontSize != null
                    && templateFontSize > 0) {
                textHeight = templateFontSize * 100;
            } else {
                textHeight = parseIntOrDefault(lineSeg.getAttribute("textheight"), 500);
            }

            int vertSize = textHeight;
            int baseline = (int) Math.round(textHeight * 0.85);
            int spacing = (int) Math.round(textHeight * 0.60);

            lineSeg.setAttribute("textheight", String.valueOf(textHeight));
            lineSeg.setAttribute("vertsize", String.valueOf(vertSize));
            lineSeg.setAttribute("baseline", String.valueOf(baseline));
            lineSeg.setAttribute("spacing", String.valueOf(spacing));
            lineSeg.setAttribute("vertpos", String.valueOf(currentVertPos));

            currentVertPos += vertSize + spacing;
        }
    }

    private Element findFirstLineSegElement(Element paragraphElement) {
        if (paragraphElement == null) {
            return null;
        }

        NodeList children = paragraphElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) {
                continue;
            }

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            boolean isLineSegArray =
                    "hp:linesegarray".equalsIgnoreCase(nodeName) ||
                    "linesegarray".equalsIgnoreCase(nodeName) ||
                    "linesegarray".equalsIgnoreCase(localName);

            if (!isLineSegArray) {
                continue;
            }

            NodeList lineSegs = el.getChildNodes();
            for (int j = 0; j < lineSegs.getLength(); j++) {
                Node segNode = lineSegs.item(j);
                if (!(segNode instanceof Element segEl)) {
                    continue;
                }

                String segName = segEl.getNodeName();
                String segLocal = segEl.getLocalName();

                boolean isLineSeg =
                        "hp:lineseg".equalsIgnoreCase(segName) ||
                        "lineseg".equalsIgnoreCase(segName) ||
                        "lineseg".equalsIgnoreCase(segLocal);

                if (isLineSeg) {
                    return segEl;
                }
            }
        }

        return null;
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Document parseXml(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(xmlBytes)));
    }

    private byte[] toBytes(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    private Align toAlignEnum(String align) {
        if (align == null) return Align.Left;

        String v = align.trim().toLowerCase();
        return switch (v) {
            case "center" -> Align.Center;
            case "right" -> Align.Right;
            default -> Align.Left;
        };
    }

    private String toRendererAlignString(String align) {
        if (align == null) return "left";

        String v = align.trim().toLowerCase();
        return switch (v) {
            case "center" -> "center";
            case "right" -> "right";
            default -> "left";
        };
    }

    private <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static class SectionRewriteResult {
        private byte[] bytes;
        private int processedStylableParagraphCount;
    }
}