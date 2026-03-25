package com.khwajeong.hwpx_saas.compose.dto;

public class StyleProfileDto {
    private Integer fontSize;
    private Boolean bold;
    private String color;
    private String align;
    private Integer spaceBefore;
    private Integer spaceAfter;
    private Integer lineSpacing;

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
}