package com.khwajeong.hwpx_saas.compose.model;

public class ExtractedTableStyle {

    // 추가:
    // 헤더 셀 텍스트 스타일
    private CellTextStyle headerTextStyle;

    // 추가:
    // 바디 셀 텍스트 스타일
    private CellTextStyle bodyTextStyle;

    // 추가:
    // 헤더 셀 배경색
    private String headerBackgroundColor;

    // 추가:
    // 바디 셀 배경색
    private String bodyBackgroundColor;

    // 추가:
    // 헤더 영역 외곽선 스타일
    private BorderStyle headerOuterBorder;

    // 추가:
    // 헤더 영역 내부선 스타일
    private BorderStyle headerInnerBorder;

    // 추가:
    // 바디 영역 외곽선 스타일
    private BorderStyle bodyOuterBorder;

    // 추가:
    // 바디 영역 내부선 스타일
    private BorderStyle bodyInnerBorder;

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

    // 추가:
    // 셀 내부 텍스트 스타일
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

    // 추가:
    // 선 스타일
    public static class BorderStyle {
        private String color;
        private Integer width;
        private String lineStyle;

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public String getLineStyle() {
            return lineStyle;
        }

        public void setLineStyle(String lineStyle) {
            this.lineStyle = lineStyle;
        }
    }
}