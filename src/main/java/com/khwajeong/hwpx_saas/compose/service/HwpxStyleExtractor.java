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

            ZipEntry entry = zipFile.getEntry("Contents/header.xml");
            if (entry == null) {
                throw new RuntimeException("header.xml not found");
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

                Map<String, Element> fontMap = buildFontMap(doc);

                StyleProfileDto style = new StyleProfileDto();

                // 문단 스타일
                Element paraPr = paraMap.get(paraPrIdRef);
                if (paraPr != null) {
                    style.setAlign(readAnyAttr(paraPr, "align", "textAlign", "horizontalAlign"));
                    style.setSpaceBefore(parseInt(readAnyAttr(paraPr, "spaceBefore", "marginTop", "before"), 0));
                    style.setSpaceAfter(parseInt(readAnyAttr(paraPr, "spaceAfter", "marginBottom", "after"), 0));
                    style.setLineSpacing(parseInt(readAnyAttr(paraPr, "lineSpacing", "lineSpacingPercent"), 160));
                }

                // 문자 스타일
                Element charPr = charMap.get(charPrIdRef);
                if (charPr != null) {
                    style.setFontSize(convertHeightToPt(readAnyAttr(charPr, "height", "fontSize")));
                    style.setBold(parseBoolean(readAnyAttr(charPr, "bold"), false));
                    style.setItalic(parseBoolean(readAnyAttr(charPr, "italic"), false));

                    String underlineRaw = readAnyAttr(charPr, "underline", "underLine");
                    style.setUnderline(parseUnderline(underlineRaw));

                    style.setColor(normalizeColor(readAnyAttr(charPr, "textColor", "color")));

                    // =========================
                    // 수정 1: 먼저 기존 속성 방식 시도
                    // =========================
                    String faceNameIdRef = readAnyAttr(
                            charPr,
                            "faceNameIdRef",
                            "faceNameRef",
                            "faceNameIds",
                            "fontRef"
                    );

                    // =========================
                    // 수정 2: 속성에 없으면 자식 노드에서 폰트 참조 찾기
                    // - 실제 로그상 charPr attr에 faceNameIdRef가 없었음
                    // - HWPX 구조에 따라 child element로 들어갈 수 있음
                    // =========================
                    if (faceNameIdRef == null || faceNameIdRef.isBlank()) {
                        faceNameIdRef = extractFontRefFromChildren(charPr);
                    }

                    String resolvedFont = resolveFontName(fontMap, faceNameIdRef);
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

    private Map<String, Element> buildFontMap(Document doc) {
        Map<String, Element> map = new HashMap<>();

        addFontNodesToMap(doc, map, "hh:font");
        addFontNodesToMap(doc, map, "hp:font");
        addFontNodesToMap(doc, map, "hh:faceName");
        addFontNodesToMap(doc, map, "hp:faceName");

        return map;
    }

    private void addFontNodesToMap(Document doc, Map<String, Element> map, String tagName) {
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
    }

    private String resolveFontName(Map<String, Element> fontMap, String faceNameIdRef) {
        if (faceNameIdRef == null || faceNameIdRef.isBlank()) {
            return null;
        }

        Element fontEl = fontMap.get(faceNameIdRef);
        if (fontEl == null) {
            return null;
        }

        // =========================
        // 수정 3: 로그상 font 노드의 실제 속성명은 "face" 였음
        // 기존엔 faceName/fontName/name만 봐서 null이 났음
        // =========================
        String fontName = readAnyAttr(fontEl, "face", "faceName", "fontName", "name");

        // 하위 노드에 이름이 있을 수도 있음
        if (fontName == null || fontName.isBlank()) {
            NodeList children = fontEl.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element childEl) {
                    fontName = readAnyAttr(childEl, "face", "faceName", "fontName", "name");
                    if (fontName != null && !fontName.isBlank()) {
                        return fontName;
                    }
                }
            }
        }

        return fontName;
    }

    // =========================
    // 수정 4: charPr 자식 노드에서 폰트 참조 ID 추출
    // - hangul 우선
    // - 없으면 latin/hanja/japanese/other 순으로 fallback
    // =========================
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
        return !"none".equals(v) && !"0".equals(v) && !"false".equals(v);
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