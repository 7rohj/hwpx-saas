package com.khwajeong.hwpx_saas.compose.model;

public class ParagraphNode {
    private String paragraphId;
    private Integer page;
    private String text;
    private Integer fontSize;
    private Boolean bold;
    private Boolean italic;
    private Boolean underline;
    private String color;
    private String align;
    private Integer spaceBefore;
    private Integer spaceAfter;
    private Integer lineSpacing;
    private Boolean singleLine;
    private String fontFamily;

    // icon 타입 문단용
    private String prefix;
    private Boolean icon;

    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphId) {
        this.paragraphId = paragraphId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public Boolean getSingleLine() {
        return singleLine;
    }

    public void setSingleLine(Boolean singleLine) {
        this.singleLine = singleLine;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Boolean getIcon() {
        return icon;
    }

    public void setIcon(Boolean icon) {
        this.icon = icon;
    }
}