package com.khwajeong.hwpx_saas.compose.dto;

public class ParagraphNodeDto {
    private String paragraphId;
    private Integer page;
    private String text;
    private Integer fontSize;
    private Boolean bold;
    private Boolean italic;      // 추가: 실제 폰트 스타일 추출용
    private Boolean underline;   // 추가: 실제 폰트 스타일 추출용
    private String color;
    private String align;
    private Integer spaceBefore;
    private Integer spaceAfter;
    private Integer lineSpacing;
    private Boolean singleLine;

    private String paraPrIdRef;
    private String charPrIdRef;

    // 추가: 실제 글꼴명 저장용
    private String fontFamily;

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

    public String getParaPrIdRef() {
        return paraPrIdRef;
    }

    public void setParaPrIdRef(String paraPrIdRef) {
        this.paraPrIdRef = paraPrIdRef;
    }

    public String getCharPrIdRef() {
        return charPrIdRef;
    }

    public void setCharPrIdRef(String charPrIdRef) {
        this.charPrIdRef = charPrIdRef;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }
}