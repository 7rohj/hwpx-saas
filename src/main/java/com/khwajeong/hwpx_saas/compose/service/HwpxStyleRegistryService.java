package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ExtractedTableStyle;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

@Service
public class HwpxStyleRegistryService {

    // 추가:
    // header xml 안에 새 charPr / paraPr / borderFill 노드를 등록하고
    // 새 ID를 반환하는 레지스트리 서비스

    public RegisteredStyleIds registerTableCellStyle(
            Document headerDoc,
            ExtractedTableStyle.CellTextStyle textStyle,
            ExtractedTableStyle.BorderStyle outerBorder,
            ExtractedTableStyle.BorderStyle innerBorder,
            String backgroundColor
    ) {
        if (headerDoc == null) {
            throw new IllegalArgumentException("headerDoc is null");
        }

        String newCharPrId = createCharPr(headerDoc, textStyle);
        String newParaPrId = createParaPr(headerDoc, textStyle);
        String newBorderFillId = createBorderFill(headerDoc, outerBorder, innerBorder, backgroundColor);

        RegisteredStyleIds ids = new RegisteredStyleIds();
        ids.setCharPrId(newCharPrId);
        ids.setParaPrId(newParaPrId);
        ids.setBorderFillId(newBorderFillId);
        return ids;
    }

    private String createCharPr(Document doc, ExtractedTableStyle.CellTextStyle textStyle) {
        Element parent = findOrCreateStyleContainer(doc, "hh:refList");
        String newId = String.valueOf(nextId(doc, "hh:charPr"));

        Element charPr = doc.createElement("hh:charPr");
        charPr.setAttribute("id", newId);

        if (textStyle != null) {
            if (textStyle.getBold() != null) {
              charPr.setAttribute("bold", textStyle.getBold() ? "1" : "0");
            }
            if (textStyle.getItalic() != null) {
              charPr.setAttribute("italic", textStyle.getItalic() ? "1" : "0");
            }
            if (textStyle.getUnderline() != null) {
              charPr.setAttribute("underline", textStyle.getUnderline() ? "solid" : "none");
            }
            if (textStyle.getFontColor() != null) {
              charPr.setAttribute("textColor", normalizeColor(textStyle.getFontColor()));
            }
            if (textStyle.getFontSize() != null) {
              charPr.setAttribute("height", String.valueOf(textStyle.getFontSize() * 100));
            }

            // 폰트 family는 실제로는 faceName 참조 id까지 맞춰야 정밀함
            // 현재는 custom marker도 같이 남겨서 후속 디버깅 가능하게 함
            if (textStyle.getFontFamily() != null) {
              charPr.setAttribute("data-font-family", textStyle.getFontFamily());
            }
        }

        parent.appendChild(charPr);
        return newId;
    }

    private String createParaPr(Document doc, ExtractedTableStyle.CellTextStyle textStyle) {
        Element parent = findOrCreateStyleContainer(doc, "hh:refList");
        String newId = String.valueOf(nextId(doc, "hh:paraPr"));

        Element paraPr = doc.createElement("hh:paraPr");
        paraPr.setAttribute("id", newId);

        if (textStyle != null && textStyle.getAlign() != null) {
            Element align = doc.createElement("hh:align");
            align.setAttribute("horizontal", textStyle.getAlign());
            paraPr.appendChild(align);
        }

        parent.appendChild(paraPr);
        return newId;
    }

    private String createBorderFill(
            Document doc,
            ExtractedTableStyle.BorderStyle outerBorder,
            ExtractedTableStyle.BorderStyle innerBorder,
            String backgroundColor
    ) {
        Element parent = findOrCreateStyleContainer(doc, "hh:refList");
        String newId = String.valueOf(nextId(doc, "hh:borderFill"));

        Element borderFill = doc.createElement("hh:borderFill");
        borderFill.setAttribute("id", newId);

        appendBorderEdge(doc, borderFill, "hh:left", outerBorder);
        appendBorderEdge(doc, borderFill, "hh:right", outerBorder);
        appendBorderEdge(doc, borderFill, "hh:top", outerBorder);
        appendBorderEdge(doc, borderFill, "hh:bottom", outerBorder);

        appendBorderEdge(doc, borderFill, "hh:horizontal", innerBorder);
        appendBorderEdge(doc, borderFill, "hh:vertical", innerBorder);

        if (backgroundColor != null && !backgroundColor.isBlank()) {
            Element fillBrush = doc.createElement("hh:fillBrush");
            Element winBrush = doc.createElement("hh:winBrush");
            winBrush.setAttribute("faceColor", normalizeColor(backgroundColor));
            winBrush.setAttribute("hatchColor", "000000");
            fillBrush.appendChild(winBrush);
            borderFill.appendChild(fillBrush);
        }

        parent.appendChild(borderFill);
        return newId;
    }

    private void appendBorderEdge(Document doc, Element parent, String tagName, ExtractedTableStyle.BorderStyle style) {
        Element edge = doc.createElement(tagName);

        if (style != null) {
            edge.setAttribute("type", style.getLineStyle() != null ? style.getLineStyle() : "solid");
            edge.setAttribute("width", style.getWidth() != null ? String.valueOf(style.getWidth()) : "1");
            edge.setAttribute("color", style.getColor() != null ? normalizeColor(style.getColor()) : "000000");
        } else {
            edge.setAttribute("type", "solid");
            edge.setAttribute("width", "1");
            edge.setAttribute("color", "000000");
        }

        parent.appendChild(edge);
    }

    private Element findOrCreateStyleContainer(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0 && list.item(0) instanceof Element el) {
            return el;
        }

        Element root = doc.getDocumentElement();
        Element created = doc.createElement(tagName);
        root.appendChild(created);
        return created;
    }

    private int nextId(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        int max = 0;

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

        return max + 1;
    }

    private String normalizeColor(String raw) {
        if (raw == null) return "000000";
        String v = raw.replace("#", "").toUpperCase();
        if (v.startsWith("00") && v.length() == 8) {
            v = v.substring(2);
        }
        return v;
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