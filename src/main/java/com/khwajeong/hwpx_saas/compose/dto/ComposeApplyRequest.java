package com.khwajeong.hwpx_saas.compose.dto;

import java.util.List;

public class ComposeApplyRequest {
    private StyleProfileDto templateHeading1Style;
    private ParagraphNodeDto sourceHeading1Sample;
    private List<ParagraphNodeDto> sourceParagraphs;

    public StyleProfileDto getTemplateHeading1Style() {
        return templateHeading1Style;
    }

    public void setTemplateHeading1Style(StyleProfileDto templateHeading1Style) {
        this.templateHeading1Style = templateHeading1Style;
    }

    public ParagraphNodeDto getSourceHeading1Sample() {
        return sourceHeading1Sample;
    }

    public void setSourceHeading1Sample(ParagraphNodeDto sourceHeading1Sample) {
        this.sourceHeading1Sample = sourceHeading1Sample;
    }

    public List<ParagraphNodeDto> getSourceParagraphs() {
        return sourceParagraphs;
    }

    public void setSourceParagraphs(List<ParagraphNodeDto> sourceParagraphs) {
        this.sourceParagraphs = sourceParagraphs;
    }
}