package com.khwajeong.hwpx_saas.compose.model;

public class StyleProfile {
    private Integer fontSize;
    private Boolean bold;
    private Boolean italic;      // 추가: 실제 스타일 적용용
    private Boolean underline;   // 추가: 실제 스타일 적용용
    private String color;
    private String align;
    private Integer spaceBefore;
    private Integer spaceAfter;
    private Integer lineSpacing;

    // 추가: 실제 글꼴명 적용용
    private String fontFamily;

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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public Integer getSpaceBefore() {
        return spaceBefore;
    }

    public void setSpaceBefore(Integer spaceBefore) {
        this.spaceBefore = spaceBefore;
    }

    public Integer getSpaceAfter() {
        return spaceAfter;
    }

    public void setSpaceAfter(Integer spaceAfter) {
        this.spaceAfter = spaceAfter;
    }

    public Integer getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(Integer lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }
}