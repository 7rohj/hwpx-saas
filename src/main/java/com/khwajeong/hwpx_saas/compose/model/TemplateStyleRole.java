package com.khwajeong.hwpx_saas.compose.model;

public class TemplateStyleRole {

    public enum RoleType {
        TITLE_L1,
        TITLE_L2,
        TITLE_L3,
        BODY,
        TABLE_HEADER,
        TABLE_BODY
    }

    private RoleType role;
    private String styleName;

    private Integer fontSize;
    private Boolean bold;
    private String color;
    private String align;
    private Integer lineSpacing;
    private Integer spaceBefore;
    private Integer spaceAfter;

    public TemplateStyleRole() {
    }

    public RoleType getRole() {
        return role;
    }

    public TemplateStyleRole setRole(RoleType role) {
        this.role = role;
        return this;
    }

    public String getStyleName() {
        return styleName;
    }

    public TemplateStyleRole setStyleName(String styleName) {
        this.styleName = styleName;
        return this;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public TemplateStyleRole setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public Boolean getBold() {
        return bold;
    }

    public TemplateStyleRole setBold(Boolean bold) {
        this.bold = bold;
        return this;
    }

    public String getColor() {
        return color;
    }

    public TemplateStyleRole setColor(String color) {
        this.color = color;
        return this;
    }

    public String getAlign() {
        return align;
    }

    public TemplateStyleRole setAlign(String align) {
        this.align = align;
        return this;
    }

    public Integer getLineSpacing() {
        return lineSpacing;
    }

    public TemplateStyleRole setLineSpacing(Integer lineSpacing) {
        this.lineSpacing = lineSpacing;
        return this;
    }

    public Integer getSpaceBefore() {
        return spaceBefore;
    }

    public TemplateStyleRole setSpaceBefore(Integer spaceBefore) {
        this.spaceBefore = spaceBefore;
        return this;
    }

    public Integer getSpaceAfter() {
        return spaceAfter;
    }

    public TemplateStyleRole setSpaceAfter(Integer spaceAfter) {
        this.spaceAfter = spaceAfter;
        return this;
    }
}