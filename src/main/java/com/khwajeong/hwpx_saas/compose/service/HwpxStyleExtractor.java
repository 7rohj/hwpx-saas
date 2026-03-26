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

                // 수정: prefix 차이 대응
                Map<String, Element> paraMap = buildMap(doc, "hh:paraPr");
                if (paraMap.isEmpty()) {
                    paraMap = buildMap(doc, "hp:paraPr");
                }

                Map<String, Element> charMap = buildMap(doc, "hh:charPr");
                if (charMap.isEmpty()) {
                    charMap = buildMap(doc, "hp:charPr");
                }

                // 수정: font/faceName 테이블 생성
                Map<String, Element> fontMap = buildFontMap(doc);

                StyleProfileDto style = new StyleProfileDto();

                // 문단 스타일 읽기
                Element paraPr = paraMap.get(paraPrIdRef);
                if (paraPr != null) {
                    style.setAlign(readAnyAttr(paraPr, "align", "textAlign", "horizontalAlign"));
                    style.setSpaceBefore(parseInt(readAnyAttr(paraPr, "spaceBefore", "marginTop", "before"), 0));
                    style.setSpaceAfter(parseInt(readAnyAttr(paraPr, "spaceAfter", "marginBottom", "after"), 0));
                    style.setLineSpacing(parseInt(readAnyAttr(paraPr, "lineSpacing", "lineSpacingPercent"), 160));
                }

                // 문자 스타일 읽기
                Element charPr = charMap.get(charPrIdRef);

                if (charPr != null) {
                    System.out.println("=== [DEBUG charPr] id=" + charPrIdRef + " ===");

                    NamedNodeMap attrs = charPr.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Node attr = attrs.item(i);
                        System.out.println("charPr attr: " + attr.getNodeName() + " = " + attr.getNodeValue());
                    }
                }

                if (charPr != null) {
                    style.setFontSize(convertHeightToPt(readAnyAttr(charPr, "height", "fontSize")));
                    style.setBold(parseBoolean(readAnyAttr(charPr, "bold"), false));
                    style.setItalic(parseBoolean(readAnyAttr(charPr, "italic"), false));

                    String underlineRaw = readAnyAttr(charPr, "underline", "underLine");
                    style.setUnderline(parseUnderline(underlineRaw));

                    style.setColor(normalizeColor(readAnyAttr(charPr, "textColor", "color")));

                    // 수정: fontFamily 추출
                    String faceNameIdRef = readAnyAttr(
                            charPr,
                            "faceNameIdRef",
                            "faceNameRef",
                            "faceNameIds",
                            "fontRef"
                    );

                    String resolvedFont = resolveFontName(fontMap, faceNameIdRef);

                    // 수정: 현재는 원본 폰트 우선
                    // fallback은 나중에 "안전 모드" 옵션으로 분리하는 게 좋음
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

    // 수정: 글꼴 테이블(font / faceName) 생성
    private Map<String, Element> buildFontMap(Document doc) {
        Map<String, Element> map = new HashMap<>();

        addFontNodesToMap(doc, map, "hh:font");
        addFontNodesToMap(doc, map, "hp:font");
        addFontNodesToMap(doc, map, "hh:faceName");
        addFontNodesToMap(doc, map, "hp:faceName");
        

        // 260326 추가
        System.out.println("=== [DEBUG fontMap] size = " + map.size() + " ===");
        for (Map.Entry<String, Element> e : map.entrySet()) {
            System.out.println("fontMap key=" + e.getKey());

            NamedNodeMap attrs = e.getValue().getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                System.out.println("  font attr: " + attr.getNodeName() + " = " + attr.getNodeValue());
            }
        }

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

    // 수정: faceNameIdRef -> 실제 폰트명
    private String resolveFontName(Map<String, Element> fontMap, String faceNameIdRef) {

        System.out.println("=== [DEBUG resolveFontName] faceNameIdRef = " + faceNameIdRef + " ===");

        if (faceNameIdRef == null || faceNameIdRef.isBlank()) {
            return null;
        }

        if (!fontMap.containsKey(faceNameIdRef)) {
            System.out.println("fontMap does not contain key: " + faceNameIdRef);
        }

        Element fontEl = fontMap.get(faceNameIdRef);
        if (fontEl == null) {
            return null;
        }

        String fontName = readAnyAttr(fontEl, "faceName", "fontName", "name");

        // 수정: 하위 노드에 name이 있는 구조도 대응
        if (fontName == null || fontName.isBlank()) {
            NodeList children = fontEl.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element childEl) {
                    fontName = readAnyAttr(childEl, "faceName", "fontName", "name");
                    if (fontName != null && !fontName.isBlank()) {
                        return fontName;
                    }
                }
            }
        }

        System.out.println("resolved fontName = " + fontName);
        return fontName;
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
        // 수정: HWPX charPr height는 보통 pt*100
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