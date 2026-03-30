package io.github.mumberrymountain.render.style;

import kr.dogfoot.hwpxlib.object.content.header_xml.HeaderXMLFile;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.FontFamilyType;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.FontType;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.Fontface;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.fontface.Font;

import java.util.ArrayList;
import java.util.List;

public class FontRenderer {
    private final HeaderXMLFile headerXMLFile;
    private final String fontFamily;
    private final List<Fontface> fontFaces = new ArrayList<>();

    public FontRenderer(HeaderXMLFile headerXMLFile, String fontFamily) {
        this.headerXMLFile = headerXMLFile;
        this.fontFamily = fontFamily;

        // 🔥 기존엔 hangul만 넣었는데, 이제 전 언어 fontface에 같은 id/font를 맞춰 넣는다
        if (headerXMLFile.refList() != null && headerXMLFile.refList().fontfaces() != null) {
            if (headerXMLFile.refList().fontfaces().hangulFontface() != null) {
                fontFaces.add(headerXMLFile.refList().fontfaces().hangulFontface());
            }
            if (headerXMLFile.refList().fontfaces().latinFontface() != null) {
                fontFaces.add(headerXMLFile.refList().fontfaces().latinFontface());
            }
            if (headerXMLFile.refList().fontfaces().hanjaFontface() != null) {
                fontFaces.add(headerXMLFile.refList().fontfaces().hanjaFontface());
            }
            if (headerXMLFile.refList().fontfaces().japaneseFontface() != null) {
                fontFaces.add(headerXMLFile.refList().fontfaces().japaneseFontface());
            }
            if (headerXMLFile.refList().fontfaces().otherFontface() != null) {
                fontFaces.add(headerXMLFile.refList().fontfaces().otherFontface());
            }
            if (headerXMLFile.refList().fontfaces().symbolFontface() != null) {
                fontFaces.add(headerXMLFile.refList().fontfaces().symbolFontface());
            }
            if (headerXMLFile.refList().fontfaces().userFontface() != null) {
                fontFaces.add(headerXMLFile.refList().fontfaces().userFontface());
            }
        }
    }

    private String checkExistingFontId(Fontface fontface) {
        if (fontface == null) return null;

        for (Font font : fontface.fonts()) {
            if (font != null && font.face() != null && font.face().equals(fontFamily)) {
                return font.id();
            }
        }
        return null;
    }

    private Font addFont(Fontface fontface, String fontId) {
        Font font = fontface.addNewFont();
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

    private String resolveSharedFontId() {
        // 1) hangul 기준으로 기존 id 찾기
        for (Fontface fontface : fontFaces) {
            String existingId = checkExistingFontId(fontface);
            if (existingId != null && !existingId.isBlank()) {
                return existingId;
            }
        }

        // 2) 없으면 hangul 기준 count를 써서 새 id 생성
        if (!fontFaces.isEmpty()) {
            Fontface first = fontFaces.get(0);
            return Integer.toString(first.countOfFont());
        }

        return "0";
    }

    private void ensureFontExistsInAllFontfaces(String sharedFontId) {
        for (Fontface fontface : fontFaces) {
            String existingId = checkExistingFontId(fontface);
            if (existingId != null && !existingId.isBlank()) {
                continue;
            }

            Font font = addFont(fontface, sharedFontId);
            setFontTypeInfo(font);
        }
    }

    public String render() {
        if (fontFamily == null || fontFamily.isBlank()) {
            return "0";
        }

        String sharedFontId = resolveSharedFontId();
        ensureFontExistsInAllFontfaces(sharedFontId);
        return sharedFontId;
    }
}