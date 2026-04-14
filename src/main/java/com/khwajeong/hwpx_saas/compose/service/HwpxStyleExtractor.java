package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class HwpxStyleExtractor {

    public StyleProfileDto extractStyle(FileWrapper fileWrapper, String paraPrIdRef, String charPrIdRef) {
        try (ZipFile zipFile = new ZipFile(fileWrapper.getFile())) {

            ZipEntry entry = findFirstHeaderEntry(zipFile);
            if (entry == null) {
                throw new RuntimeException("header xml not found");
            }

            try (InputStream in = zipFile.getInputStream(entry)) {
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(in);

                Map<String, Element> paraMap = buildMap(doc, "hh:paraPr");
                if (paraMap.isEmpty()) {
                    paraMap = buildMap(doc, "hp:paraPr");
                }

                Map<String, Element> charMap = buildMap(doc, "hh:charPr");
                if (charMap.isEmpty()) {
                    charMap = buildMap(doc, "hp:charPr");
                }

                StyleProfileDto style = new StyleProfileDto();

                Element paraPr = paraMap.get(paraPrIdRef);
                if (paraPr != null) {
                    style.setAlign(extractAlign(paraPr));
                    style.setSpaceBefore(extractSpaceBefore(paraPr));
                    style.setSpaceAfter(extractSpaceAfter(paraPr));
                    style.setLineSpacing(extractLineSpacing(paraPr));
                }

                Element charPr = charMap.get(charPrIdRef);
                if (charPr != null) {
                    style.setFontSize(convertHeightToPt(readAnyAttr(charPr, "height", "fontSize")));
                    style.setBold(extractBold(charPr));
                    style.setItalic(extractItalic(charPr));

                    // 밑줄은 태그 존재 여부가 아니라 실제 값/속성으로 판정
                    style.setUnderline(extractUnderline(charPr));

                    style.setColor(normalizeColor(readAnyAttr(charPr, "textColor", "color")));

                    String hangulFontId = extractFontRefByLang(charPr, "hangul");
                    if (hangulFontId == null || hangulFontId.isBlank()) {
                        hangulFontId = extractFontRefByLang(charPr, "latin");
                    }
                    if (hangulFontId == null || hangulFontId.isBlank()) {
                        hangulFontId = extractFontRefFromChildren(charPr);
                    }

                    String resolvedFont = resolveFontName(doc, hangulFontId, "HANGUL");
                    if (resolvedFont == null || resolvedFont.isBlank()) {
                        resolvedFont = resolveFontName(doc, hangulFontId, "LATIN");
                    }
                    if (resolvedFont == null || resolvedFont.isBlank()) {
                        resolvedFont = resolveFontNameGlobal(doc, hangulFontId);
                    }

                    style.setFontFamily(resolvedFont);
                }

                return style;
            }

        } catch (Exception e) {
            throw new RuntimeException("style extraction failed", e);
        }
    }

    private Map<String, Element> buildMap(Document doc, String tagName) {
        Map<String, Element> map = new HashMap<>();

        NodeList list = doc.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (!(node instanceof Element el)) {
                continue;
            }

            String id = readAnyAttr(el, "id");
            if (id != null && !id.isBlank()) {
                map.put(id, el);
            }
        }

        return map;
    }

    private ZipEntry findFirstHeaderEntry(ZipFile zipFile) {
        return zipFile.stream()
                .filter(e -> !e.isDirectory())
                .filter(e -> e.getName().startsWith("Contents/header"))
                .filter(e -> e.getName().endsWith(".xml"))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .findFirst()
                .orElse(null);
    }

    private String resolveFontName(Document doc, String fontId, String lang) {
        if (fontId == null || fontId.isBlank()) {
            return null;
        }

        NodeList fontfaceList = doc.getElementsByTagName("hh:fontface");
        for (int i = 0; i < fontfaceList.getLength(); i++) {
            Node node = fontfaceList.item(i);
            if (!(node instanceof Element fontfaceEl)) {
                continue;
            }

            String currentLang = readAnyAttr(fontfaceEl, "lang");
            if (currentLang == null || !currentLang.equalsIgnoreCase(lang)) {
                continue;
            }

            NodeList children = fontfaceEl.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (!(child instanceof Element childEl)) {
                    continue;
                }

                String nodeName = childEl.getNodeName();
                if (!"hh:font".equalsIgnoreCase(nodeName) && !"hp:font".equalsIgnoreCase(nodeName)) {
                    continue;
                }

                String id = readAnyAttr(childEl, "id");
                if (fontId.equals(id)) {
                    String fontName = readAnyAttr(childEl, "face", "faceName", "fontName", "name");
                    if (fontName != null && !fontName.isBlank()) {
                        return fontName;
                    }
                }
            }
        }

        return null;
    }

    private String resolveFontNameGlobal(Document doc, String fontId) {
        if (fontId == null || fontId.isBlank()) {
            return null;
        }

        String[] tags = {"hh:font", "hp:font", "hh:faceName", "hp:faceName"};
        for (String tag : tags) {
            NodeList list = doc.getElementsByTagName(tag);
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                if (!(node instanceof Element el)) {
                    continue;
                }

                String id = readAnyAttr(el, "id");
                if (!fontId.equals(id)) {
                    continue;
                }

                String fontName = readAnyAttr(el, "face", "faceName", "fontName", "name");
                if (fontName != null && !fontName.isBlank()) {
                    return fontName;
                }
            }
        }

        return null;
    }

    private String extractFontRefByLang(Element charPr, String langAttrName) {
        if (charPr == null) {
            return null;
        }

        NodeList children = charPr.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element childEl)) {
                continue;
            }

            String nodeName = childEl.getNodeName();
            String localName = childEl.getLocalName();

            boolean looksLikeFontRef =
                    "fontRef".equalsIgnoreCase(nodeName) ||
                    "fontRef".equalsIgnoreCase(localName) ||
                    nodeName.toLowerCase().contains("fontref") ||
                    (localName != null && localName.toLowerCase().contains("fontref"));

            if (!looksLikeFontRef) {
                continue;
            }

            String ref = readAnyAttr(childEl, langAttrName);
            if (ref != null && !ref.isBlank()) {
                return ref;
            }
        }

        return null;
    }

    private String resolveNodeAttr(Element parent, String childTag, String attrName) {
        if (parent == null) return null;

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element childEl)) {
                continue;
            }

            String nodeName = childEl.getNodeName();
            String localName = childEl.getLocalName();

            if (childTag.equalsIgnoreCase(nodeName) || childTag.equalsIgnoreCase(localName)) {
                String value = readAnyAttr(childEl, attrName);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }

        return null;
    }

    private int extractLineSpacing(Element paraPr) {
        String fromAttr = readAnyAttr(paraPr, "lineSpacing", "lineSpacingPercent");
        if (fromAttr != null && !fromAttr.isBlank()) {
            return parseInt(fromAttr, 160);
        }

        NodeList children = paraPr.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element childEl)) {
                continue;
            }

            String nodeName = childEl.getNodeName();
            String localName = childEl.getLocalName();

            if ("lineSpacing".equalsIgnoreCase(nodeName) || "lineSpacing".equalsIgnoreCase(localName)
                    || "hh:lineSpacing".equalsIgnoreCase(nodeName) || "hp:lineSpacing".equalsIgnoreCase(nodeName)) {
                String value = readAnyAttr(childEl, "value");
                return parseInt(value, 160);
            }
        }

        return 160;
    }

    private String extractAlign(Element paraPr) {
        String fromAttr = readAnyAttr(paraPr, "align", "textAlign", "horizontalAlign");
        if (fromAttr != null && !fromAttr.isBlank()) {
            return fromAttr;
        }

        NodeList children = paraPr.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element childEl)) {
                continue;
            }

            String nodeName = childEl.getNodeName();
            String localName = childEl.getLocalName();

            if ("align".equalsIgnoreCase(nodeName) || "align".equalsIgnoreCase(localName)
                    || "hh:align".equalsIgnoreCase(nodeName) || "hp:align".equalsIgnoreCase(nodeName)) {
                String horizontal = readAnyAttr(childEl, "horizontal");
                if (horizontal != null && !horizontal.isBlank()) {
                    return horizontal;
                }
            }
        }

        return null;
    }

    private int extractSpaceBefore(Element paraPr) {
        return extractMarginValue(paraPr, "prev", 0);
    }

    private int extractSpaceAfter(Element paraPr) {
        return extractMarginValue(paraPr, "next", 0);
    }

    private int extractMarginValue(Element paraPr, String target, int def) {
        String fromAttr = readAnyAttr(
                paraPr,
                "spaceBefore",
                "marginTop",
                "before",
                "spaceAfter",
                "marginBottom",
                "after"
        );
        if (fromAttr != null && !fromAttr.isBlank()) {
            return parseInt(fromAttr, def);
        }

        NodeList children = paraPr.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element childEl)) {
                continue;
            }

            String nodeName = childEl.getNodeName();
            String localName = childEl.getLocalName();

            boolean isMargin =
                    "margin".equalsIgnoreCase(nodeName) ||
                    "margin".equalsIgnoreCase(localName) ||
                    "hh:margin".equalsIgnoreCase(nodeName) ||
                    "hp:margin".equalsIgnoreCase(nodeName);

            if (!isMargin) {
                continue;
            }

            NodeList marginChildren = childEl.getChildNodes();
            for (int j = 0; j < marginChildren.getLength(); j++) {
                Node marginNode = marginChildren.item(j);
                if (!(marginNode instanceof Element marginEl)) {
                    continue;
                }

                String mNodeName = marginEl.getNodeName();
                String mLocalName = marginEl.getLocalName();

                if (target.equalsIgnoreCase(mNodeName) || target.equalsIgnoreCase(mLocalName)) {
                    String value = readAnyAttr(marginEl, "value");
                    return parseInt(value, def);
                }
            }
        }

        return def;
    }

    private boolean extractBold(Element charPr) {
        String raw = readAnyAttr(charPr, "bold");
        if (raw != null) {
            return parseBoolean(raw, false);
        }
        return hasChild(charPr, "hh:bold") || hasChild(charPr, "hp:bold");
    }

        private boolean extractItalic(Element charPr) {
        String raw = readAnyAttr(charPr, "italic");
        if (raw != null) {
            return parseBoolean(raw, false);
        }
        return hasChild(charPr, "hh:italic") || hasChild(charPr, "hp:italic");
    }

    /**
     * 밑줄 추출
     *
     * 기존 문제:
     * - underline 속성이 비어 있으면
     * - underline 자식 태그가 "존재하기만 해도" true 처리됨
     *
     * 수정:
     * 1) charPr 자체 속성 underline / underLine 우선
     * 2) 없으면 underline 자식의 type / shape / lineStyle / style / value 확인
     * 3) none / false / 0 류면 false
     * 4) 실제 underline 스타일일 때만 true
     */
    private boolean extractUnderline(Element charPr) {
        String raw = readAnyAttr(charPr, "underline", "underLine");
        if (raw != null && !raw.isBlank()) {
            return parseUnderline(raw);
        }

        Element underlineEl = findChild(charPr, "hh:underline", "hp:underline", "underline");
        if (underlineEl == null) {
            return false;
        }

        String childRaw = readAnyAttr(
                underlineEl,
                "type",
                "shape",
                "lineStyle",
                "style",
                "value",
                "underline",
                "underLine"
        );

        if (childRaw != null && !childRaw.isBlank()) {
            return parseUnderline(childRaw);
        }

        // 태그가 있어도 값이 없으면 기본 false
        return false;
    }

    private Element findChild(Element parent, String... childTagNames) {
        if (parent == null || childTagNames == null) {
            return null;
        }

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) {
                continue;
            }

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            for (String tag : childTagNames) {
                if (tag.equalsIgnoreCase(nodeName) || tag.equalsIgnoreCase(localName)) {
                    return el;
                }
            }
        }

        return null;
    }

    private boolean hasChild(Element parent, String childTagName) {
        if (parent == null) return false;

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) {
                continue;
            }

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();
            if (childTagName.equalsIgnoreCase(nodeName) || childTagName.equalsIgnoreCase(localName)) {
                return true;
            }
        }

        return false;
    }

    private String extractFontRefFromChildren(Element charPr) {
        NodeList children = charPr.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element childEl)) {
                continue;
            }

            String nodeName = childEl.getNodeName();
            String localName = childEl.getLocalName();

            boolean looksLikeFontRef =
                    "fontRef".equalsIgnoreCase(nodeName) ||
                    "fontRef".equalsIgnoreCase(localName) ||
                    nodeName.toLowerCase().contains("fontref") ||
                    (localName != null && localName.toLowerCase().contains("fontref"));

            if (!looksLikeFontRef) {
                continue;
            }

            String ref = readAnyAttr(
                    childEl,
                    "hangul",
                    "korean",
                    "latin",
                    "hanja",
                    "japanese",
                    "other",
                    "idRef",
                    "fontId"
            );

            if (ref != null && !ref.isBlank()) {
                return ref;
            }
        }

        return null;
    }

    private String readAnyAttr(Element el, String... names) {
        if (el == null || names == null) {
            return null;
        }

        NamedNodeMap attrs = el.getAttributes();
        if (attrs == null) {
            return null;
        }

        for (String name : names) {
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                if (attr == null) {
                    continue;
                }

                String nodeName = attr.getNodeName();
                String localName = attr.getLocalName();

                if (name.equalsIgnoreCase(nodeName) || name.equalsIgnoreCase(localName)) {
                    return attr.getNodeValue();
                }
            }
        }

        return null;
    }

    private int convertHeightToPt(String raw) {
        int value = parseInt(raw, 1200);

        if (value >= 100) {
            return Math.round(value / 100.0f);
        }
        return value;
    }

    private boolean parseBoolean(String raw, boolean def) {
        if (raw == null) {
            return def;
        }
        String v = raw.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "y".equals(v);
    }

        private boolean parseUnderline(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }

        String v = raw.trim().toLowerCase();

        return !(
                "none".equals(v) ||
                "noneunderline".equals(v) ||
                "no".equals(v) ||
                "false".equals(v) ||
                "0".equals(v) ||
                "null".equals(v) ||
                "none_under_line".equals(v)
        );
    }

    private String normalizeColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String v = raw.trim();
        if (!v.startsWith("#") && v.matches("[0-9A-Fa-f]{6,8}")) {
            return "#" + v;
        }
        return v;
    }

    private int parseInt(String v, int def) {
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return def;
        }
    }

    public static class FileWrapper {
        private final File file;

        public FileWrapper(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }
    }
}