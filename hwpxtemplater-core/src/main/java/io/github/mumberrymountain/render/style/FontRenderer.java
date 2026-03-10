package io.github.mumberrymountain.render.style;

import kr.dogfoot.hwpxlib.object.content.header_xml.HeaderXMLFile;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.FontFamilyType;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.FontType;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.Fontface;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.fontface.Font;

public class FontRenderer {
    private final HeaderXMLFile headerXMLFile;
    private final String fontFamily;
    private final Fontface fontFace;

    public FontRenderer (HeaderXMLFile headerXMLFile, String fontFamily) {
        this.headerXMLFile = headerXMLFile;
        this.fontFamily = fontFamily;
        this.fontFace = headerXMLFile.refList().fontfaces().hangulFontface();
    }

    private String checkExistingFonts(Fontface fontface) {
        String fontId = null;
        for (Font font : fontface.fonts()) {
            if (font.face().equals(fontFamily)) fontId = font.id();
        }
        return fontId;
    }

    private Font setFont(String fontId) {
        Font font = fontFace.addNewFont();
        font.id(fontId);
        font.type(FontType.TTF);
        font.isEmbedded(false);
        font.face(fontFamily);
        return font;
    }

    private void setFontTypeInfo(Font font) {
        font.createTypeInfo();
        font.typeInfo().familyType(FontFamilyType.FCAT_UNKNOWN);
        font.typeInfo().weight(0);
        font.typeInfo().proportion(0);
        font.typeInfo().contrast(0);
        font.typeInfo().strokeVariation(0);
        font.typeInfo().armStyle(false);
        font.typeInfo().letterform(false);
        font.typeInfo().midline(252);
        font.typeInfo().xHeight(255);
    }

    private String addNewFont() {
        String fontId = Integer.toString(fontFace.countOfFont());
        setFontTypeInfo(setFont(fontId));

        return fontId;
    }

    public String render(){
        String fontId = checkExistingFonts(fontFace);
        if (fontId == null) fontId = addNewFont();
        return fontId;
    }
}
