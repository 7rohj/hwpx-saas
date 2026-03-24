package com.khwajeong.hwpx_saas.compose.model;

public class ExtractedStyle {

    private TemplateRoleType roleType;

    private String fontFamily;
    private Integer fontSize;
    private Boolean bold;
    private String fontColor;

    private String align;
    private Integer lineSpacingPercent;
    private Integer spaceBefore;
    private Integer spaceAfter;

    // 🔥 다음 단계용: 템플릿에서 실제로 뽑은 base style id
    private String baseParaPrId;
    private String baseCharPrId;
    private String sampleText;

    public TemplateRoleType getRoleType() {
        return roleType;
    }

    public ExtractedStyle setRoleType(TemplateRoleType roleType) {
        this.roleType = roleType;
        return this;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public ExtractedStyle setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public ExtractedStyle setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public Boolean getBold() {
        return bold;
    }

    public ExtractedStyle setBold(Boolean bold) {
        this.bold = bold;
        return this;
    }

    public String getFontColor() {
        return fontColor;
    }

    public ExtractedStyle setFontColor(String fontColor) {
        this.fontColor = fontColor;
        return this;
    }

    public String getAlign() {
        return align;
    }

    public ExtractedStyle setAlign(String align) {
        this.align = align;
        return this;
    }

    public Integer getLineSpacingPercent() {
        return lineSpacingPercent;
    }

    public ExtractedStyle setLineSpacingPercent(Integer lineSpacingPercent) {
        this.lineSpacingPercent = lineSpacingPercent;
        return this;
    }

    public Integer getSpaceBefore() {
        return spaceBefore;
    }

    public ExtractedStyle setSpaceBefore(Integer spaceBefore) {
        this.spaceBefore = spaceBefore;
        return this;
    }

    public Integer getSpaceAfter() {
        return spaceAfter;
    }

    public ExtractedStyle setSpaceAfter(Integer spaceAfter) {
        this.spaceAfter = spaceAfter;
        return this;
    }

    public String getBaseParaPrId() {
        return baseParaPrId;
    }

    public ExtractedStyle setBaseParaPrId(String baseParaPrId) {
        this.baseParaPrId = baseParaPrId;
        return this;
    }

    public String getBaseCharPrId() {
        return baseCharPrId;
    }

    public ExtractedStyle setBaseCharPrId(String baseCharPrId) {
        this.baseCharPrId = baseCharPrId;
        return this;
    }

    public String getSampleText() {
        return sampleText;
    }

    public ExtractedStyle setSampleText(String sampleText) {
        this.sampleText = sampleText;
        return this;
    }
}