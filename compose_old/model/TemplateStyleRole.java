package com.khwajeong.hwpx_saas.compose.model;

public class TemplateStyleRole {

    private TemplateRoleType roleType;
    private String sampleText;
    private String styleName;

    public TemplateStyleRole() {
    }

    public TemplateRoleType getRoleType() {
        return roleType;
    }

    public TemplateStyleRole setRoleType(TemplateRoleType roleType) {
        this.roleType = roleType;
        return this;
    }

    public String getSampleText() {
        return sampleText;
    }

    public TemplateStyleRole setSampleText(String sampleText) {
        this.sampleText = sampleText;
        return this;
    }

    public String getStyleName() {
        return styleName;
    }

    public TemplateStyleRole setStyleName(String styleName) {
        this.styleName = styleName;
        return this;
    }

    @Override
    public String toString() {
        return "TemplateStyleRole{" +
                "roleType=" + roleType +
                ", sampleText='" + sampleText + '\'' +
                ", styleName='" + styleName + '\'' +
                '}';
    }
}