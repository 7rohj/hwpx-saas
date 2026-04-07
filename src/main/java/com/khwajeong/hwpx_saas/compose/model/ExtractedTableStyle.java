package com.khwajeong.hwpx_saas.compose.model;

public class ExtractedTableStyle {

    private CellTextStyle headerTextStyle;
    private CellTextStyle bodyTextStyle;

    private String headerBackgroundColor;
    private String bodyBackgroundColor;

    private BorderStyle headerOuterBorder;
    private BorderStyle headerInnerBorder;

    private BorderStyle bodyOuterBorder;
    private BorderStyle bodyInnerBorder;

    private BorderStyle bodyLastRowBorder;

    public CellTextStyle getHeaderTextStyle() {
        return headerTextStyle;
    }

    public void setHeaderTextStyle(CellTextStyle headerTextStyle) {
        this.headerTextStyle = headerTextStyle;
    }

    public CellTextStyle getBodyTextStyle() {
        return bodyTextStyle;
    }

    public void setBodyTextStyle(CellTextStyle bodyTextStyle) {
        this.bodyTextStyle = bodyTextStyle;
    }

    public String getHeaderBackgroundColor() {
        return headerBackgroundColor;
    }

    public void setHeaderBackgroundColor(String headerBackgroundColor) {
        this.headerBackgroundColor = headerBackgroundColor;
    }

    public String getBodyBackgroundColor() {
        return bodyBackgroundColor;
    }

    public void setBodyBackgroundColor(String bodyBackgroundColor) {
        this.bodyBackgroundColor = bodyBackgroundColor;
    }

    public BorderStyle getHeaderOuterBorder() {
        return headerOuterBorder;
    }

    public void setHeaderOuterBorder(BorderStyle headerOuterBorder) {
        this.headerOuterBorder = headerOuterBorder;
    }

    public BorderStyle getHeaderInnerBorder() {
        return headerInnerBorder;
    }

    public void setHeaderInnerBorder(BorderStyle headerInnerBorder) {
        this.headerInnerBorder = headerInnerBorder;
    }

    public BorderStyle getBodyOuterBorder() {
        return bodyOuterBorder;
    }

    public void setBodyOuterBorder(BorderStyle bodyOuterBorder) {
        this.bodyOuterBorder = bodyOuterBorder;
    }

    public BorderStyle getBodyInnerBorder() {
        return bodyInnerBorder;
    }

    public void setBodyInnerBorder(BorderStyle bodyInnerBorder) {
        this.bodyInnerBorder = bodyInnerBorder;
    }

    public BorderStyle getBodyLastRowBorder() {
        return bodyLastRowBorder;
    }

    public void setBodyLastRowBorder(BorderStyle bodyLastRowBorder) {
        this.bodyLastRowBorder = bodyLastRowBorder;
    }

    public static class CellTextStyle {
        private String fontFamily;
        private Integer fontSize;
        private Boolean bold;
        private Boolean italic;
        private Boolean underline;
        private String fontColor;
        private String align;

        public String getFontFamily() {
            return fontFamily;
        }

        public void setFontFamily(String fontFamily) {
            this.fontFamily = fontFamily;
        }

        public Integer getFontSize() {
            return fontSize;
        }

        public void setFontSize(Integer fontSize) {
            this.fontSize = fontSize;
        }

        public Boolean getBold() {
            return bold;
        }

        public void setBold(Boolean bold) {
            this.bold = bold;
        }

        public Boolean getItalic() {
            return italic;
        }

        public void setItalic(Boolean italic) {
            this.italic = italic;
        }

        public Boolean getUnderline() {
            return underline;
        }

        public void setUnderline(Boolean underline) {
            this.underline = underline;
        }

        public String getFontColor() {
            return fontColor;
        }

        public void setFontColor(String fontColor) {
            this.fontColor = fontColor;
        }

        public String getAlign() {
            return align;
        }

        public void setAlign(String align) {
            this.align = align;
        }
    }

    public static class BorderStyle {
        private String lineStyle;
        private String width;   // 핵심: Integer -> String 으로 변경
        private String color;

        public String getLineStyle() {
            return lineStyle;
        }

        public void setLineStyle(String lineStyle) {
            this.lineStyle = lineStyle;
        }

        public String getWidth() {
            return width;
        }

        public void setWidth(String width) {
            this.width = width;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }
}