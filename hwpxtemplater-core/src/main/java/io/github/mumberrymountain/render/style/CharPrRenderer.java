package io.github.mumberrymountain.render.style;

import kr.dogfoot.hwpxlib.object.content.header_xml.HeaderXMLFile;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.*;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.CharPr;
import io.github.mumberrymountain.model.Text;

public class CharPrRenderer {
    private final HeaderXMLFile headerXMLFile;
    private final int fontSize;
    private final String fontColor;
    private final String backgroundColor;
    private final String fontId;
    private final CharPr charPr;
    private final boolean bold;
    private final boolean italic;
    private final boolean underLine;
    private final boolean strikeOut;
    private final boolean outline;
    private final boolean shadow;
    private final boolean emboss;
    private final boolean engrave;

    private final String script;

    public CharPrRenderer(HeaderXMLFile headerXMLFile, String fontId, Text text) {
        this.headerXMLFile = headerXMLFile;
        this.fontId = fontId;

        // 🔥 중요:
        // HWPX charPr.height 는 보통 1000 = 10pt, 1200 = 12pt, 2000 = 20pt 식으로 간다.
        // 기존 pxToHwpxUnit 을 쓰면 20pt가 15처럼 틀어질 수 있어서 pt * 100 방식으로 고정.
        this.fontSize = toHwpCharHeight(text.getFontSize());

        this.fontColor = normalizeColor(text.getFontColor(), "#000000");
        this.backgroundColor = normalizeColor(text.getBackgroundColor(), "#FFFFFFFF");
        this.bold = text.isBold();
        this.italic = text.isItalic();
        this.underLine = text.isUnderLine();
        this.strikeOut = text.isStrikeOut();
        this.outline = text.isOutline();
        this.shadow = text.isShadow();
        this.emboss = text.isEmboss();
        this.engrave = text.isEngrave();
        this.script = text.getScript();
        this.charPr = new CharPr();
    }

    private int toHwpCharHeight(int pt) {
        int safePt = pt <= 0 ? 12 : pt;
        return safePt * 100;
    }

    private String normalizeColor(String color, String fallback) {
        if (color == null || color.isBlank()) {
            return fallback;
        }

        String v = color.trim();
        if (!v.startsWith("#")) {
            if (v.matches("[0-9A-Fa-f]{6,8}")) {
                return "#" + v;
            }
            return fallback;
        }
        return v;
    }

    private void setId() {
        String charPrId = Integer.toString(headerXMLFile.refList().charProperties().count());
        charPr.id(charPrId);
    }

    private void setCharPr() {
        charPr.height(fontSize);
        charPr.textColor(fontColor);
        charPr.shadeColor(backgroundColor);
        charPr.useFontSpace(false);
        charPr.useKerning(false);
    }

    private void setFontRef() {
        charPr.createFontRef();
        charPr.fontRef().hangul(fontId);
        charPr.fontRef().latin(fontId);
        charPr.fontRef().hanja(fontId);
        charPr.fontRef().japanese(fontId);
        charPr.fontRef().other(fontId);
        charPr.fontRef().symbol(fontId);
        charPr.fontRef().user(fontId);
    }

    private void setRatio() {
        charPr.createRatio();
        charPr.ratio().hangul((short) 100);
        charPr.ratio().latin((short) 100);
        charPr.ratio().hanja((short) 100);
        charPr.ratio().japanese((short) 100);
        charPr.ratio().other((short) 100);
        charPr.ratio().symbol((short) 100);
        charPr.ratio().user((short) 100);
    }

    private void setSpacing() {
        charPr.createSpacing();
        charPr.spacing().hangul((short) 0);
        charPr.spacing().latin((short) 0);
        charPr.spacing().hanja((short) 0);
        charPr.spacing().japanese((short) 0);
        charPr.spacing().other((short) 0);
        charPr.spacing().symbol((short) 0);
        charPr.spacing().user((short) 0);
    }

    private void setRelSz() {
        charPr.createRelSz();
        charPr.relSz().hangul((short) 100);
        charPr.relSz().latin((short) 100);
        charPr.relSz().hanja((short) 100);
        charPr.relSz().japanese((short) 100);
        charPr.relSz().other((short) 100);
        charPr.relSz().symbol((short) 100);
        charPr.relSz().user((short) 100);
    }

    private void setOffset() {
        charPr.createOffset();

        short offset = 0;

        if (script != null) {
            String s = script.trim().toLowerCase();
            if (s.equals("superscript") || s.equals("super") || s.equals("up")) {
                offset = 30;
            } else if (s.equals("subscript") || s.equals("sub") || s.equals("down")) {
                offset = -30;
            }
        }

        charPr.offset().hangul(offset);
        charPr.offset().latin(offset);
        charPr.offset().hanja(offset);
        charPr.offset().japanese(offset);
        charPr.offset().other(offset);
        charPr.offset().symbol(offset);
        charPr.offset().user(offset);
    }

    private void setBold() {
        charPr.createBold();
    }

    private void setItalic() {
        charPr.createItalic();
    }

    private void setUnderLine() {
        charPr.createUnderline();
        charPr.underline().type(UnderlineType.BOTTOM);
        charPr.underline().shape(LineType3.SOLID);
        charPr.underline().color(fontColor != null ? fontColor : "#000000");
    }

    private void setStrikeOut() {
        charPr.createStrikeout();
        charPr.strikeout().shape(LineType2.SOLID);
        charPr.strikeout().color(fontColor != null ? fontColor : "#000000");
    }

    private void setOutline() {
        charPr.createOutline();
        charPr.outline().type(LineType1.SOLID);
    }

    private void setShadow() {
        charPr.createShadow();
        charPr.shadow().type(CharShadowType.DROP);
        charPr.shadow().color("#B2B2B2");
        charPr.shadow().offsetX((short) 10);
        charPr.shadow().offsetY((short) 10);
    }

    private void setEmboss() {
        charPr.createEmboss();
    }

    private void setEngrave() {
        charPr.createEngrave();
    }

    public CharPr render() {
        setId();
        setCharPr();
        setFontRef();
        setRatio();
        setSpacing();
        setRelSz();
        setOffset();

        if (bold) setBold();
        if (italic) setItalic();
        if (underLine) setUnderLine();
        if (strikeOut) setStrikeOut();
        if (outline) setOutline();
        if (shadow) setShadow();
        if (emboss) setEmboss();
        if (engrave) setEngrave();

        return charPr;
    }
}