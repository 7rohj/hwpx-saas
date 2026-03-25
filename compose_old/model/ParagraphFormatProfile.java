package com.khwajeong.hwpx_saas.compose.model;

public class ParagraphFormatProfile {

    private TemplateRoleType roleType;
    private String sampleText;
    private String paraPrId;
    private String charPrId;

    public TemplateRoleType getRoleType() {
        return roleType;
    }

    public ParagraphFormatProfile setRoleType(TemplateRoleType roleType) {
        this.roleType = roleType;
        return this;
    }

    public String getSampleText() {
        return sampleText;
    }

    public ParagraphFormatProfile setSampleText(String sampleText) {
        this.sampleText = sampleText;
        return this;
    }

    public String getParaPrId() {
        return paraPrId;
    }

    public ParagraphFormatProfile setParaPrId(String paraPrId) {
        this.paraPrId = paraPrId;
        return this;
    }

    public String getCharPrId() {
        return charPrId;
    }

    public ParagraphFormatProfile setCharPrId(String charPrId) {
        this.charPrId = charPrId;
        return this;
    }
}