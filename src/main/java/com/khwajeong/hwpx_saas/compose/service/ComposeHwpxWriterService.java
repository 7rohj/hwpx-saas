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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class ComposeHwpxWriterService {

    public Path writeStyledCopy(
            Path templateHwpxPath,
            Path sourceHwpxPath,
            String templateParaPrIdRef,
            String templateCharPrIdRef,
            List<String> matchedParagraphIds
    ) throws Exception {

        if (templateHwpxPath == null || !Files.exists(templateHwpxPath)) {
            throw new IllegalArgumentException("template hwpx not found");
        }

        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }

        if (matchedParagraphIds == null || matchedParagraphIds.isEmpty()) {
            throw new IllegalArgumentException("matchedParagraphIds is empty");
        }

        Set<Integer> targetIndexes = new LinkedHashSet<>();
        for (String id : matchedParagraphIds) {
            if (id == null) continue;
            String s = id.trim();
            if (s.startsWith("p")) s = s.substring(1);
            try {
                targetIndexes.add(Integer.parseInt(s));
            } catch (Exception ignored) {
            }
        }

        Path outputPath = Files.createTempFile("compose-writer-output-", ".hwpx");

        TemplateStyleBundle bundle = loadTemplateStyleBundle(
                templateHwpxPath,
                templateParaPrIdRef,
                templateCharPrIdRef
        );

        int globalParagraphIndex = 0;

        try (
                ZipFile sourceZip = new ZipFile(sourceHwpxPath.toFile());
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

                if (entryName.startsWith("Contents/header") && entryName.endsWith(".xml")) {
                    HeaderMergeResult mergeResult = mergeStyleIntoHeader(originalBytes, bundle);
                    outputBytes = mergeResult.bytes;
                    bundle.mergedParaPrIdRef = mergeResult.newParaPrIdRef;
                    bundle.mergedCharPrIdRef = mergeResult.newCharPrIdRef;
                    bundle.mergedBorderFillIdRef = mergeResult.newBorderFillIdRef;
                }

                if (entryName.startsWith("Contents/section") && entryName.endsWith(".xml")) {
                    SectionRewriteResult result = rewriteSectionXml(
                            originalBytes,
                            globalParagraphIndex,
                            targetIndexes,
                            bundle.mergedParaPrIdRef,
                            bundle.mergedCharPrIdRef,
                            bundle.mergedBorderFillIdRef
                    );
                    outputBytes = result.bytes;
                    globalParagraphIndex += result.processedParagraphCount;
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

    private TemplateStyleBundle loadTemplateStyleBundle(
            Path templateHwpxPath,
            String templateParaPrIdRef,
            String templateCharPrIdRef
    ) throws Exception {

        try (ZipFile templateZip = new ZipFile(templateHwpxPath.toFile())) {
            ZipEntry headerEntry = findFirstHeaderEntry(templateZip);
            if (headerEntry == null) {
                throw new IllegalArgumentException("template header xml not found");
            }

            byte[] headerBytes;
            try (InputStream in = templateZip.getInputStream(headerEntry)) {
                headerBytes = in.readAllBytes();
            }

            Document doc = parseXml(headerBytes);

            Element paraPrEl = findElementById(doc, "hh:paraPr", templateParaPrIdRef);
            if (paraPrEl == null) {
                paraPrEl = findElementById(doc, "hp:paraPr", templateParaPrIdRef);
            }

            Element charPrEl = findElementById(doc, "hh:charPr", templateCharPrIdRef);
            if (charPrEl == null) {
                charPrEl = findElementById(doc, "hp:charPr", templateCharPrIdRef);
            }

            if (paraPrEl == null) {
                throw new IllegalArgumentException("template paraPr not found: " + templateParaPrIdRef);
            }
            if (charPrEl == null) {
                throw new IllegalArgumentException("template charPr not found: " + templateCharPrIdRef);
            }

            TemplateStyleBundle bundle = new TemplateStyleBundle();
            bundle.templateParaPrElement = paraPrEl;
            bundle.templateCharPrElement = charPrEl;
            bundle.templateFontRefs = extractFontRefs(charPrEl);
            bundle.templateHeaderDoc = doc;
            bundle.templateHeaderBorderFillIdRef = extractFirstTableHeaderBorderFillId(templateHwpxPath);

            return bundle;
        }
    }

    private HeaderMergeResult mergeStyleIntoHeader(byte[] headerXmlBytes, TemplateStyleBundle bundle) throws Exception {
        Document sourceDoc = parseXml(headerXmlBytes);

        Map<String, String> fontRefRemap = mergeReferencedFonts(
                sourceDoc,
                bundle.templateHeaderDoc,
                bundle.templateFontRefs
        );

        Element refList = findOrCreateRefList(sourceDoc);

        String newParaPrId = nextId(sourceDoc, bundle.templateParaPrElement.getTagName());
        Element clonedParaPr = (Element) sourceDoc.importNode(bundle.templateParaPrElement, true);
        clonedParaPr.setAttribute("id", newParaPrId);
        refList.appendChild(clonedParaPr);

        String newCharPrId = nextId(sourceDoc, bundle.templateCharPrElement.getTagName());
        Element clonedCharPr = (Element) sourceDoc.importNode(bundle.templateCharPrElement, true);
        clonedCharPr.setAttribute("id", newCharPrId);
        remapCharPrFontRefs(clonedCharPr, fontRefRemap);
        refList.appendChild(clonedCharPr);

        String newBorderFillIdRef = null;
        if (bundle.templateHeaderBorderFillIdRef != null && !bundle.templateHeaderBorderFillIdRef.isBlank()) {
            newBorderFillIdRef = copyBorderFillIntoHeader(
                    sourceDoc,
                    bundle.templateHeaderDoc,
                    bundle.templateHeaderBorderFillIdRef
            );
        }

        HeaderMergeResult result = new HeaderMergeResult();
        result.newParaPrIdRef = newParaPrId;
        result.newCharPrIdRef = newCharPrId;
        result.newBorderFillIdRef = newBorderFillIdRef;
        result.bytes = toBytes(sourceDoc);
        return result;
    }

    private String copyBorderFillIntoHeader(
            Document sourceHeaderDoc,
            Document templateHeaderDoc,
            String templateBorderFillIdRef
    ) {
        if (sourceHeaderDoc == null || templateHeaderDoc == null ||
                templateBorderFillIdRef == null || templateBorderFillIdRef.isBlank()) {
            return null;
        }

        Element templateBorderFill = findElementById(templateHeaderDoc, "hh:borderFill", templateBorderFillIdRef);
        if (templateBorderFill == null) {
            templateBorderFill = findElementById(templateHeaderDoc, "hp:borderFill", templateBorderFillIdRef);
        }
        if (templateBorderFill == null) {
            return null;
        }

        Element sourceBorderFills = findOrCreateBorderFills(sourceHeaderDoc);
        String newId = nextBorderFillId(sourceHeaderDoc);

        Element cloned = (Element) sourceHeaderDoc.importNode(templateBorderFill, true);
        cloned.setAttribute("id", newId);
        sourceBorderFills.appendChild(cloned);

        updateBorderFillsItemCnt(sourceBorderFills);

        return newId;
    }

    private Element findOrCreateBorderFills(Document doc) {
        NodeList list = doc.getElementsByTagName("hh:borderFills");
        if (list.getLength() > 0 && list.item(0) instanceof Element el) {
            return el;
        }

        NodeList hpList = doc.getElementsByTagName("hp:borderFills");
        if (hpList.getLength() > 0 && hpList.item(0) instanceof Element el) {
            return el;
        }

        Element refList = findOrCreateRefList(doc);
        Element borderFills = doc.createElement("hh:borderFills");
        borderFills.setAttribute("itemCnt", "0");
        refList.appendChild(borderFills);
        return borderFills;
    }

    private void updateBorderFillsItemCnt(Element borderFillsEl) {
        if (borderFillsEl == null) return;

        int count = 0;
        NodeList children = borderFillsEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element el) {
                String nodeName = el.getNodeName();
                String localName = el.getLocalName();

                boolean isBorderFill =
                        "hh:borderFill".equalsIgnoreCase(nodeName) ||
                        "hp:borderFill".equalsIgnoreCase(nodeName) ||
                        "borderFill".equalsIgnoreCase(localName);

                if (isBorderFill) {
                    count++;
                }
            }
        }

        borderFillsEl.setAttribute("itemCnt", String.valueOf(count));
    }

    private String extractFirstTableHeaderBorderFillId(Path templateHwpxPath) throws Exception {
        try (ZipFile templateZip = new ZipFile(templateHwpxPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = templateZip.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!name.startsWith("Contents/section") || !name.endsWith(".xml")) {
                    continue;
                }

                byte[] bytes;
                try (InputStream in = templateZip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }

                Document doc = parseXml(bytes);
                NodeList tblList = doc.getElementsByTagName("hp:tbl");
                if (tblList.getLength() == 0) continue;

                Element tbl = (Element) tblList.item(0);
                NodeList trList = tbl.getElementsByTagName("hp:tr");
                if (trList.getLength() == 0) continue;

                Element firstRow = (Element) trList.item(0);
                NodeList tcList = firstRow.getElementsByTagName("hp:tc");
                if (tcList.getLength() == 0) continue;

                Element firstCell = (Element) tcList.item(0);
                String borderFillId = firstCell.getAttribute("borderFillIDRef");

                if (borderFillId != null && !borderFillId.isBlank()) {
                    return borderFillId;
                }
            }
        }

        return null;
    }

    private SectionRewriteResult rewriteSectionXml(
            byte[] xmlBytes,
            int globalStartIndex,
            Set<Integer> targetIndexes,
            String sourceParaPrIdRef,
            String sourceCharPrIdRef,
            String mergedBorderFillIdRef
    ) throws Exception {

        Document doc = parseXml(xmlBytes);

        applyHeaderBorderFillToAllTables(doc, mergedBorderFillIdRef);

        NodeList paraList = doc.getElementsByTagName("hp:p");
        int paraCount = paraList.getLength();

        int logicalParagraphIndex = globalStartIndex;

        for (int i = 0; i < paraCount; i++) {
            Element p = (Element) paraList.item(i);

            boolean hasText = hasTextRun(p);

            if (!hasText) {
                continue;
            }

            if (targetIndexes.contains(logicalParagraphIndex)) {
                if (sourceParaPrIdRef != null && !sourceParaPrIdRef.isBlank()) {
                    p.setAttribute("paraPrIDRef", sourceParaPrIdRef);
                }

                if (sourceCharPrIdRef != null && !sourceCharPrIdRef.isBlank()) {
                    rewriteAllCharPrRefs(p, sourceCharPrIdRef);
                }

                removeLineSegArray(p);
            }

            logicalParagraphIndex++;
        }

        SectionRewriteResult result = new SectionRewriteResult();
        result.bytes = toBytes(doc);
        result.processedParagraphCount = logicalParagraphIndex - globalStartIndex;
        return result;
    }

    private void applyHeaderBorderFillToAllTables(Document doc, String borderFillIdRef) {
        if (doc == null || borderFillIdRef == null || borderFillIdRef.isBlank()) {
            return;
        }

        NodeList tblList = doc.getElementsByTagName("hp:tbl");

        for (int i = 0; i < tblList.getLength(); i++) {
            Node tblNode = tblList.item(i);
            if (!(tblNode instanceof Element tbl)) continue;

            NodeList trList = tbl.getElementsByTagName("hp:tr");
            if (trList.getLength() == 0) continue;

            Element firstRow = (Element) trList.item(0);
            NodeList tcList = firstRow.getElementsByTagName("hp:tc");

            for (int j = 0; j < tcList.getLength(); j++) {
                Node tcNode = tcList.item(j);
                if (tcNode instanceof Element tc) {
                    tc.setAttribute("borderFillIDRef", borderFillIdRef);
                }
            }
        }
    }

    private Map<String, String> mergeReferencedFonts(
            Document sourceHeaderDoc,
            Document templateHeaderDoc,
            Map<String, String> templateFontRefs
    ) {
        Map<String, String> remap = new LinkedHashMap<>();

        for (Map.Entry<String, String> e : templateFontRefs.entrySet()) {
            String lang = e.getKey();
            String oldFontId = e.getValue();

            if (oldFontId == null || oldFontId.isBlank()) continue;

            Element templateFontface = findFontfaceByLang(templateHeaderDoc, lang);
            if (templateFontface == null) continue;

            Element templateFont = findFontInFontfaceById(templateFontface, oldFontId);
            if (templateFont == null) continue;

            Element sourceFontface = findOrCreateFontfaceByLang(sourceHeaderDoc, lang);
            String existingId = findFontIdByFace(sourceFontface, safeAttr(templateFont, "face"));

            String newFontId;
            if (existingId != null && !existingId.isBlank()) {
                newFontId = existingId;
            } else {
                newFontId = nextFontIdInFontface(sourceFontface);
                Element clonedFont = (Element) sourceHeaderDoc.importNode(templateFont, true);
                clonedFont.setAttribute("id", newFontId);
                sourceFontface.appendChild(clonedFont);
            }

            remap.put(lang + "|" + oldFontId, newFontId);
        }

        return remap;
    }

    private Map<String, String> extractFontRefs(Element charPrEl) {
        Map<String, String> refs = new LinkedHashMap<>();
        if (charPrEl == null) return refs;

        NodeList children = charPrEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            boolean isFontRef =
                    "hh:fontRef".equalsIgnoreCase(nodeName) ||
                    "hp:fontRef".equalsIgnoreCase(nodeName) ||
                    "fontRef".equalsIgnoreCase(nodeName) ||
                    "fontRef".equalsIgnoreCase(localName);

            if (!isFontRef) continue;

            copyAttrIfExists(el, refs, "hangul");
            copyAttrIfExists(el, refs, "latin");
            copyAttrIfExists(el, refs, "hanja");
            copyAttrIfExists(el, refs, "japanese");
            copyAttrIfExists(el, refs, "other");
            copyAttrIfExists(el, refs, "symbol");
            copyAttrIfExists(el, refs, "user");
        }

        if (refs.isEmpty()) {
            copyAttrIfExists(charPrEl, refs, "hangul");
            copyAttrIfExists(charPrEl, refs, "latin");
            copyAttrIfExists(charPrEl, refs, "hanja");
            copyAttrIfExists(charPrEl, refs, "japanese");
            copyAttrIfExists(charPrEl, refs, "other");
            copyAttrIfExists(charPrEl, refs, "symbol");
            copyAttrIfExists(charPrEl, refs, "user");
        }

        return refs;
    }

    private void copyAttrIfExists(Element el, Map<String, String> refs, String lang) {
        String value = safeAttr(el, lang);
        if (value != null && !value.isBlank()) {
            refs.put(lang, value);
        }
    }

    private void remapCharPrFontRefs(Element charPrEl, Map<String, String> remap) {
        if (charPrEl == null || remap == null || remap.isEmpty()) return;

        NodeList children = charPrEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            boolean isFontRef =
                    "hh:fontRef".equalsIgnoreCase(nodeName) ||
                    "hp:fontRef".equalsIgnoreCase(nodeName) ||
                    "fontRef".equalsIgnoreCase(nodeName) ||
                    "fontRef".equalsIgnoreCase(localName);

            if (!isFontRef) continue;

            remapFontRefAttr(el, remap, "hangul");
            remapFontRefAttr(el, remap, "latin");
            remapFontRefAttr(el, remap, "hanja");
            remapFontRefAttr(el, remap, "japanese");
            remapFontRefAttr(el, remap, "other");
            remapFontRefAttr(el, remap, "symbol");
            remapFontRefAttr(el, remap, "user");
        }
    }

    private void remapFontRefAttr(Element fontRefEl, Map<String, String> remap, String lang) {
        String oldId = safeAttr(fontRefEl, lang);
        if (oldId == null || oldId.isBlank()) return;

        String newId = remap.get(lang + "|" + oldId);
        if (newId != null && !newId.isBlank()) {
            fontRefEl.setAttribute(lang, newId);
        }
    }

    private Element findFontfaceByLang(Document doc, String lang) {
        for (String tag : List.of("hh:fontface", "hp:fontface")) {
            NodeList list = doc.getElementsByTagName(tag);
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (!(node instanceof Element el)) continue;

                String currentLang = safeAttr(el, "lang");
                if (currentLang != null && currentLang.equalsIgnoreCase(toHwpxLang(lang))) {
                    return el;
                }
            }
        }
        return null;
    }

    private Element findOrCreateFontfaceByLang(Document doc, String lang) {
        Element found = findFontfaceByLang(doc, lang);
        if (found != null) return found;

        Element refList = findOrCreateRefList(doc);

        Element fontface = doc.createElement("hh:fontface");
        fontface.setAttribute("lang", toHwpxLang(lang));
        refList.appendChild(fontface);
        return fontface;
    }

    private String toHwpxLang(String lang) {
        return switch (lang.toLowerCase()) {
            case "hangul" -> "HANGUL";
            case "latin" -> "LATIN";
            case "hanja" -> "HANJA";
            case "japanese" -> "JAPANESE";
            case "other" -> "OTHER";
            case "symbol" -> "SYMBOL";
            case "user" -> "USER";
            default -> lang.toUpperCase();
        };
    }

    private Element findFontInFontfaceById(Element fontfaceEl, String id) {
        NodeList children = fontfaceEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            boolean isFont =
                    "hh:font".equalsIgnoreCase(nodeName) ||
                    "hp:font".equalsIgnoreCase(nodeName) ||
                    "font".equalsIgnoreCase(nodeName) ||
                    "font".equalsIgnoreCase(localName);

            if (!isFont) continue;

            if (id.equals(safeAttr(el, "id"))) {
                return el;
            }
        }
        return null;
    }

    private String findFontIdByFace(Element fontfaceEl, String face) {
        if (fontfaceEl == null || face == null || face.isBlank()) return null;

        NodeList children = fontfaceEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            boolean isFont =
                    "hh:font".equalsIgnoreCase(nodeName) ||
                    "hp:font".equalsIgnoreCase(nodeName) ||
                    "font".equalsIgnoreCase(nodeName) ||
                    "font".equalsIgnoreCase(localName);

            if (!isFont) continue;

            if (face.equals(safeAttr(el, "face"))) {
                return safeAttr(el, "id");
            }
        }
        return null;
    }

    private String nextFontIdInFontface(Element fontfaceEl) {
        int max = -1;

        NodeList children = fontfaceEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            boolean isFont =
                    "hh:font".equalsIgnoreCase(nodeName) ||
                    "hp:font".equalsIgnoreCase(nodeName) ||
                    "font".equalsIgnoreCase(nodeName) ||
                    "font".equalsIgnoreCase(localName);

            if (!isFont) continue;

            String id = safeAttr(el, "id");
            if (id == null || id.isBlank()) continue;

            try {
                max = Math.max(max, Integer.parseInt(id));
            } catch (Exception ignored) {
            }
        }

        return String.valueOf(max + 1);
    }

    private boolean hasTextRun(Element p) {
        if (p == null) return false;

        NodeList runList = p.getElementsByTagName("hp:run");
        for (int i = 0; i < runList.getLength(); i++) {
            Node runNode = runList.item(i);
            if (!(runNode instanceof Element runEl)) continue;

            NodeList textNodes = runEl.getElementsByTagName("hp:t");
            for (int j = 0; j < textNodes.getLength(); j++) {
                Node tNode = textNodes.item(j);
                String text = tNode.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void rewriteAllCharPrRefs(Element root, String newCharPrIdRef) {
        if (root == null || newCharPrIdRef == null || newCharPrIdRef.isBlank()) {
            return;
        }

        if (root.hasAttribute("charPrIDRef")) {
            root.setAttribute("charPrIDRef", newCharPrIdRef);
        }

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element el) {
                rewriteAllCharPrRefs(el, newCharPrIdRef);
            }
        }
    }

    private void removeLineSegArray(Element p) {
        NodeList children = p.getChildNodes();
        List<Node> toRemove = new ArrayList<>();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element el) {
                String nodeName = el.getNodeName();
                String localName = el.getLocalName();

                boolean isLineSegArray =
                        "hp:linesegarray".equalsIgnoreCase(nodeName) ||
                        "linesegarray".equalsIgnoreCase(nodeName) ||
                        "linesegarray".equalsIgnoreCase(localName);

                if (isLineSegArray) {
                    toRemove.add(el);
                }
            }
        }

        for (Node node : toRemove) {
            p.removeChild(node);
        }
    }

    private Document parseXml(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(xmlBytes)));
    }

    private ZipEntry findFirstHeaderEntry(ZipFile zipFile) {
        return zipFile.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().startsWith("Contents/header"))
                .filter(e -> e.getName().endsWith(".xml"))
                .sorted(Comparator.comparing(ZipEntry::getName))
                .findFirst()
                .orElse(null);
    }

    private Element findElementById(Document doc, String tagName, String id) {
        if (id == null || id.isBlank()) return null;

        NodeList list = doc.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (!(node instanceof Element el)) continue;
            if (id.equals(el.getAttribute("id"))) return el;
        }
        return null;
    }

    private Element findOrCreateRefList(Document doc) {
        for (String tag : List.of("hh:refList", "hp:refList")) {
            NodeList list = doc.getElementsByTagName(tag);
            if (list.getLength() > 0 && list.item(0) instanceof Element el) {
                return el;
            }
        }

        Element root = doc.getDocumentElement();
        Element refList = doc.createElement("hh:refList");
        root.appendChild(refList);
        return refList;
    }

    private String nextId(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        int max = -1;

        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (!(node instanceof Element el)) continue;

            String id = el.getAttribute("id");
            if (id == null || id.isBlank()) continue;

            try {
                max = Math.max(max, Integer.parseInt(id));
            } catch (Exception ignored) {
            }
        }

        return String.valueOf(max + 1);
    }

    private String nextBorderFillId(Document doc) {
        int max = -1;

        for (String containerTag : List.of("hh:borderFills", "hp:borderFills")) {
            NodeList containers = doc.getElementsByTagName(containerTag);
            for (int c = 0; c < containers.getLength(); c++) {
                Node containerNode = containers.item(c);
                if (!(containerNode instanceof Element containerEl)) continue;

                NodeList children = containerEl.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (!(node instanceof Element el)) continue;

                    String nodeName = el.getNodeName();
                    String localName = el.getLocalName();

                    boolean isBorderFill =
                            "hh:borderFill".equalsIgnoreCase(nodeName) ||
                            "hp:borderFill".equalsIgnoreCase(nodeName) ||
                            "borderFill".equalsIgnoreCase(localName);

                    if (!isBorderFill) continue;

                    String id = el.getAttribute("id");
                    if (id == null || id.isBlank()) continue;

                    try {
                        max = Math.max(max, Integer.parseInt(id));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return String.valueOf(max + 1);
    }

    private String safeAttr(Element el, String name) {
        if (el == null || name == null) return null;
        String v = el.getAttribute(name);
        return (v == null || v.isBlank()) ? null : v;
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

    private static class SectionRewriteResult {
        private byte[] bytes;
        private int processedParagraphCount;
    }

    private static class HeaderMergeResult {
        private byte[] bytes;
        private String newParaPrIdRef;
        private String newCharPrIdRef;
        private String newBorderFillIdRef;
    }

    private static class TemplateStyleBundle {
        private Element templateParaPrElement;
        private Element templateCharPrElement;
        private Map<String, String> templateFontRefs = new LinkedHashMap<>();
        private Document templateHeaderDoc;
        private String mergedParaPrIdRef;
        private String mergedCharPrIdRef;
        private String templateHeaderBorderFillIdRef;
        private String mergedBorderFillIdRef;
    }
}