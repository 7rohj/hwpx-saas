package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ExtractedTableStyle;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

@Service
public class HwpxStyleRegistryService {

    public RegisteredStyleIds registerTableCellStyle(
            Document headerDoc,
            ExtractedTableStyle.CellTextStyle textStyle,
            ExtractedTableStyle.BorderStyle outerBorder,
            ExtractedTableStyle.BorderStyle innerHorizontalBorder,
            String backgroundColor
    ) {
        if (headerDoc == null) {
            throw new IllegalArgumentException("headerDoc is null");
        }

        String newCharPrId = createCharPr(headerDoc, textStyle);
        String newParaPrId = createParaPr(headerDoc, textStyle);
        String newBorderFillId = createBorderFill(
                headerDoc,
                outerBorder,              // left/right
                innerHorizontalBorder,    // top
                innerHorizontalBorder,    // bottom
                backgroundColor
        );

        RegisteredStyleIds ids = new RegisteredStyleIds();
        ids.setCharPrId(newCharPrId);
        ids.setParaPrId(newParaPrId);
        ids.setBorderFillId(newBorderFillId);
        return ids;
    }

    public String registerCustomBorderFill(
            Document headerDoc,
            ExtractedTableStyle.BorderStyle leftRightBorder,
            ExtractedTableStyle.BorderStyle topBorder,
            ExtractedTableStyle.BorderStyle bottomBorder,
            String backgroundColor
    ) {
        return createBorderFill(headerDoc, leftRightBorder, topBorder, bottomBorder, backgroundColor);
    }

    private String createCharPr(Document doc, ExtractedTableStyle.CellTextStyle textStyle) {
        Element parent = findOrCreateCharProperties(doc);
        String newId = nextId(parent, "hh:charPr");

        Element charPr = doc.createElement("hh:charPr");
        charPr.setAttribute("id", newId);
        charPr.setAttribute("shadeColor", "#FFFFFFFF");
        charPr.setAttribute("useFontSpace", "0");
        charPr.setAttribute("useKerning", "0");
        charPr.setAttribute("symMark", "NONE");
        charPr.setAttribute("borderFillIDRef", "3");

        if (textStyle != null) {
            charPr.setAttribute("textColor", textStyle.getFontColor() != null ? normalizeColor(textStyle.getFontColor()) : "#000000");
            charPr.setAttribute("height", textStyle.getFontSize() != null ? String.valueOf(textStyle.getFontSize() * 100) : "1000");

            if (Boolean.TRUE.equals(textStyle.getBold())) {
                charPr.appendChild(doc.createElement("hh:bold"));
            }

            FontRefIds ids = registerFontFamily(doc, textStyle.getFontFamily());
            Element fontRef = doc.createElement("hh:fontRef");
            fontRef.setAttribute("hangul", ids.hangulId);
            fontRef.setAttribute("latin", ids.latinId);
            fontRef.setAttribute("hanja", ids.hanjaId);
            fontRef.setAttribute("japanese", ids.japaneseId);
            fontRef.setAttribute("other", ids.otherId);
            fontRef.setAttribute("symbol", ids.symbolId);
            fontRef.setAttribute("user", ids.userId);
            charPr.appendChild(fontRef);

            charPr.appendChild(createCommonTextSubNode(doc, "hh:ratio", "100"));
            charPr.appendChild(createCommonTextSubNode(doc, "hh:spacing", "0"));
            charPr.appendChild(createCommonTextSubNode(doc, "hh:relSz", "100"));
            charPr.appendChild(createCommonTextSubNode(doc, "hh:offset", "0"));
        }

        parent.appendChild(charPr);
        updateItemCnt(parent);
        return newId;
    }

    private Element createCommonTextSubNode(Document doc, String tagName, String value) {
        Element el = doc.createElement(tagName);
        el.setAttribute("hangul", value);
        el.setAttribute("latin", value);
        el.setAttribute("hanja", value);
        el.setAttribute("japanese", value);
        el.setAttribute("other", value);
        el.setAttribute("symbol", value);
        el.setAttribute("user", value);
        return el;
    }

    private String createParaPr(Document doc, ExtractedTableStyle.CellTextStyle textStyle) {
        Element parent = findOrCreateParaProperties(doc);
        String newId = nextId(parent, "hh:paraPr");

        Element paraPr = doc.createElement("hh:paraPr");
        paraPr.setAttribute("id", newId);
        paraPr.setAttribute("tabPrIDRef", "0");
        paraPr.setAttribute("condense", "0");
        paraPr.setAttribute("fontLineHeight", "0");
        paraPr.setAttribute("snapToGrid", "1");
        paraPr.setAttribute("suppressLineNumbers", "0");
        paraPr.setAttribute("checked", "0");

        Element align = doc.createElement("hh:align");
        align.setAttribute("horizontal", normalizeHorizontalAlign(textStyle != null ? textStyle.getAlign() : null));
        align.setAttribute("vertical", "BASELINE");
        paraPr.appendChild(align);

        Element heading = doc.createElement("hh:heading");
        heading.setAttribute("type", "NONE");
        heading.setAttribute("idRef", "0");
        heading.setAttribute("level", "0");
        paraPr.appendChild(heading);

        Element breakSetting = doc.createElement("hh:breakSetting");
        breakSetting.setAttribute("breakLatinWord", "KEEP_WORD");
        breakSetting.setAttribute("breakNonLatinWord", "BREAK_WORD");
        breakSetting.setAttribute("widowOrphan", "0");
        breakSetting.setAttribute("keepWithNext", "0");
        breakSetting.setAttribute("keepLines", "0");
        breakSetting.setAttribute("pageBreakBefore", "0");
        breakSetting.setAttribute("lineWrap", "BREAK");
        paraPr.appendChild(breakSetting);

        Element autoSpacing = doc.createElement("hh:autoSpacing");
        autoSpacing.setAttribute("eAsianEng", "0");
        autoSpacing.setAttribute("eAsianNum", "0");
        paraPr.appendChild(autoSpacing);

        Element margin = doc.createElement("hh:margin");
        margin.appendChild(createMarginNode(doc, "hc:intent", "0"));
        margin.appendChild(createMarginNode(doc, "hc:left", "0"));
        margin.appendChild(createMarginNode(doc, "hc:right", "0"));
        margin.appendChild(createMarginNode(doc, "hc:prev", "0"));
        margin.appendChild(createMarginNode(doc, "hc:next", "0"));
        paraPr.appendChild(margin);

        Element lineSpacing = doc.createElement("hh:lineSpacing");
        lineSpacing.setAttribute("type", "PERCENT");
        lineSpacing.setAttribute("value", "160");
        lineSpacing.setAttribute("unit", "HWPUNIT");
        paraPr.appendChild(lineSpacing);

        Element border = doc.createElement("hh:border");
        border.setAttribute("borderFillIDRef", "3");
        border.setAttribute("offsetLeft", "0");
        border.setAttribute("offsetRight", "0");
        border.setAttribute("offsetTop", "0");
        border.setAttribute("offsetBottom", "0");
        border.setAttribute("connect", "0");
        border.setAttribute("ignoreMargin", "0");
        paraPr.appendChild(border);

        parent.appendChild(paraPr);
        updateItemCnt(parent);
        return newId;
    }

    private Element createMarginNode(Document doc, String tagName, String value) {
        Element el = doc.createElement(tagName);
        el.setAttribute("value", value);
        el.setAttribute("unit", "HWPUNIT");
        return el;
    }

    private String createBorderFill(
            Document doc,
            ExtractedTableStyle.BorderStyle leftRightBorder,
            ExtractedTableStyle.BorderStyle topBorder,
            ExtractedTableStyle.BorderStyle bottomBorder,
            String backgroundColor
    ) {
        Element parent = findOrCreateBorderFills(doc);
        String newId = nextId(parent, "hh:borderFill");

        Element borderFill = doc.createElement("hh:borderFill");
        borderFill.setAttribute("id", newId);
        borderFill.setAttribute("threeD", "0");
        borderFill.setAttribute("shadow", "0");
        borderFill.setAttribute("centerLine", "NONE");
        borderFill.setAttribute("breakCellSeparateLine", "0");

        Element slash = doc.createElement("hh:slash");
        slash.setAttribute("type", "NONE");
        slash.setAttribute("Crooked", "0");
        slash.setAttribute("isCounter", "0");
        borderFill.appendChild(slash);

        Element backSlash = doc.createElement("hh:backSlash");
        backSlash.setAttribute("type", "NONE");
        backSlash.setAttribute("Crooked", "0");
        backSlash.setAttribute("isCounter", "0");
        borderFill.appendChild(backSlash);

        appendBorderEdge(doc, borderFill, "hh:leftBorder", leftRightBorder);
        appendBorderEdge(doc, borderFill, "hh:rightBorder", leftRightBorder);
        appendBorderEdge(doc, borderFill, "hh:topBorder", topBorder);
        appendBorderEdge(doc, borderFill, "hh:bottomBorder", bottomBorder);

        Element diagonal = doc.createElement("hh:diagonal");
        diagonal.setAttribute("type", "SOLID");
        diagonal.setAttribute("width", "0.1 mm");
        diagonal.setAttribute("color", "#000000");
        borderFill.appendChild(diagonal);

        if (backgroundColor != null && !backgroundColor.isBlank()) {
            Element fillBrush = doc.createElement("hc:fillBrush");
            Element winBrush = doc.createElement("hc:winBrush");
            winBrush.setAttribute("faceColor", normalizeColor(backgroundColor));
            winBrush.setAttribute("hatchColor", "#000000");
            winBrush.setAttribute("alpha", "0");
            fillBrush.appendChild(winBrush);
            borderFill.appendChild(fillBrush);
        }

        parent.appendChild(borderFill);
        updateItemCnt(parent);
        return newId;
    }

    private void appendBorderEdge(
            Document doc,
            Element parent,
            String tagName,
            ExtractedTableStyle.BorderStyle style
    ) {
        Element edge = doc.createElement(tagName);

        String lineStyle = "SOLID";
        String width = "0.12 mm";
        String color = "#000000";

        if (style != null) {
            if (style.getLineStyle() != null && !style.getLineStyle().isBlank()) {
                lineStyle = style.getLineStyle().toUpperCase();
            }
            if (style.getWidth() != null && !style.getWidth().isBlank()) {
                width = style.getWidth(); // template 원문 그대로
            }
            if (style.getColor() != null && !style.getColor().isBlank()) {
                color = normalizeColor(style.getColor());
            }
        }

        edge.setAttribute("type", lineStyle);
        edge.setAttribute("width", width);
        edge.setAttribute("color", color);
        parent.appendChild(edge);
    }

    private FontRefIds registerFontFamily(Document doc, String fontFamily) {
        String safe = (fontFamily == null || fontFamily.isBlank()) ? "한컴바탕" : fontFamily;

        FontRefIds ids = new FontRefIds();
        ids.hangulId = findOrCreateFont(doc, "HANGUL", safe);
        ids.latinId = findOrCreateFont(doc, "LATIN", safe);
        ids.hanjaId = findOrCreateFont(doc, "HANJA", safe);
        ids.japaneseId = findOrCreateFont(doc, "JAPANESE", safe);
        ids.otherId = findOrCreateFont(doc, "OTHER", safe);
        ids.symbolId = findOrCreateFont(doc, "SYMBOL", safe);
        ids.userId = findOrCreateFont(doc, "USER", safe);
        return ids;
    }

    private String findOrCreateFont(Document doc, String lang, String face) {
        Element fontFaceEl = findOrCreateFontFace(doc, lang);

        NodeList children = fontFaceEl.getChildNodes();
        int maxId = 0;

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element child)) continue;

            String nodeName = child.getNodeName();
            String localName = child.getLocalName();

            boolean isFont =
                    "hh:font".equalsIgnoreCase(nodeName) ||
                    "font".equalsIgnoreCase(nodeName) ||
                    "font".equalsIgnoreCase(localName);

            if (!isFont) continue;

            String existingFace = child.getAttribute("face");
            String existingId = child.getAttribute("id");

            if (face.equals(existingFace) && existingId != null && !existingId.isBlank()) {
                return existingId;
            }

            try {
                maxId = Math.max(maxId, Integer.parseInt(existingId));
            } catch (Exception ignored) {
            }
        }

        String newId = String.valueOf(maxId + 1);

        Element fontEl = doc.createElement("hh:font");
        fontEl.setAttribute("id", newId);
        fontEl.setAttribute("face", face);
        fontEl.setAttribute("type", "TTF");
        fontEl.setAttribute("isEmbedded", "0");
        fontFaceEl.appendChild(fontEl);
        updateFontCnt(fontFaceEl);

        return newId;
    }

    private Element findOrCreateFontFace(Document doc, String lang) {
        NodeList list = doc.getElementsByTagName("hh:fontface");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (!(node instanceof Element el)) continue;

            String currentLang = el.getAttribute("lang");
            if (lang.equalsIgnoreCase(currentLang)) {
                return el;
            }
        }

        Element fontfaces = findOrCreateFontfaces(doc);
        Element fontFace = doc.createElement("hh:fontface");
        fontFace.setAttribute("lang", lang);
        fontFace.setAttribute("fontCnt", "0");
        fontfaces.appendChild(fontFace);
        updateItemCnt(fontfaces);
        return fontFace;
    }

    private Element findOrCreateFontfaces(Document doc) {
        NodeList list = doc.getElementsByTagName("hh:fontfaces");
        if (list.getLength() > 0 && list.item(0) instanceof Element el) {
            return el;
        }
        Element refList = findOrCreateRefList(doc);
        Element created = doc.createElement("hh:fontfaces");
        created.setAttribute("itemCnt", "0");
        refList.appendChild(created);
        return created;
    }

    private Element findOrCreateBorderFills(Document doc) {
        NodeList list = doc.getElementsByTagName("hh:borderFills");
        if (list.getLength() > 0 && list.item(0) instanceof Element el) {
            return el;
        }
        Element refList = findOrCreateRefList(doc);
        Element created = doc.createElement("hh:borderFills");
        created.setAttribute("itemCnt", "0");
        refList.appendChild(created);
        return created;
    }

    private Element findOrCreateCharProperties(Document doc) {
        NodeList list = doc.getElementsByTagName("hh:charProperties");
        if (list.getLength() > 0 && list.item(0) instanceof Element el) {
            return el;
        }
        Element refList = findOrCreateRefList(doc);
        Element created = doc.createElement("hh:charProperties");
        created.setAttribute("itemCnt", "0");
        refList.appendChild(created);
        return created;
    }

    private Element findOrCreateParaProperties(Document doc) {
        NodeList list = doc.getElementsByTagName("hh:paraProperties");
        if (list.getLength() > 0 && list.item(0) instanceof Element el) {
            return el;
        }
        Element refList = findOrCreateRefList(doc);
        Element created = doc.createElement("hh:paraProperties");
        created.setAttribute("itemCnt", "0");
        refList.appendChild(created);
        return created;
    }

    private Element findOrCreateRefList(Document doc) {
        NodeList list = doc.getElementsByTagName("hh:refList");
        if (list.getLength() > 0 && list.item(0) instanceof Element el) {
            return el;
        }

        Element root = doc.getDocumentElement();
        Element created = doc.createElement("hh:refList");
        root.appendChild(created);
        return created;
    }

    private String nextId(Element parent, String childTagName) {
        NodeList list = parent.getChildNodes();
        int max = 0;

        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();
            boolean match =
                    childTagName.equalsIgnoreCase(nodeName) ||
                    childTagName.equalsIgnoreCase(localName);

            if (!match) continue;

            String id = el.getAttribute("id");
            if (id == null || id.isBlank()) continue;

            try {
                max = Math.max(max, Integer.parseInt(id));
            } catch (Exception ignored) {
            }
        }

        return String.valueOf(max + 1);
    }

    private void updateItemCnt(Element collectionEl) {
        int count = 0;
        NodeList list = collectionEl.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i) instanceof Element) {
                count++;
            }
        }
        collectionEl.setAttribute("itemCnt", String.valueOf(count));
    }

    private void updateFontCnt(Element fontFaceEl) {
        int count = 0;
        NodeList list = fontFaceEl.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i) instanceof Element el) {
                String nodeName = el.getNodeName();
                String localName = el.getLocalName();
                if ("hh:font".equalsIgnoreCase(nodeName) || "font".equalsIgnoreCase(localName)) {
                    count++;
                }
            }
        }
        fontFaceEl.setAttribute("fontCnt", String.valueOf(count));
    }

    private String normalizeColor(String raw) {
        if (raw == null || raw.isBlank()) return "#000000";
        String v = raw.trim().toUpperCase().replace("##", "#");
        if (!v.startsWith("#")) {
            v = "#" + v.replace("#", "");
        }
        return v;
    }

    private String normalizeHorizontalAlign(String raw) {
        if (raw == null || raw.isBlank()) {
            return "CENTER";
        }

        String v = raw.trim().toLowerCase();
        return switch (v) {
            case "right" -> "RIGHT";
            case "justify" -> "JUSTIFY";
            case "left" -> "LEFT";
            default -> "CENTER";
        };
    }

    private static class FontRefIds {
        private String hangulId;
        private String latinId;
        private String hanjaId;
        private String japaneseId;
        private String otherId;
        private String symbolId;
        private String userId;
    }

    public static class RegisteredStyleIds {
        private String charPrId;
        private String paraPrId;
        private String borderFillId;

        public String getCharPrId() {
            return charPrId;
        }

        public void setCharPrId(String charPrId) {
            this.charPrId = charPrId;
        }

        public String getParaPrId() {
            return paraPrId;
        }

        public void setParaPrId(String paraPrId) {
            this.paraPrId = paraPrId;
        }

        public String getBorderFillId() {
            return borderFillId;
        }

        public void setBorderFillId(String borderFillId) {
            this.borderFillId = borderFillId;
        }
    }
}