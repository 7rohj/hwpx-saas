package com.khwajeong.hwpx_saas.compose.dto;

import java.util.List;

public class ComposeRunApplyResponse {
    private String sessionId;
    private String label;
    private String templateSelectionSummary;
    private String sourceSelectionSummary;
    private List<String> matchedParagraphIds;
    private List<ParagraphNodeDto> styledParagraphs;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getTemplateSelectionSummary() {
        return templateSelectionSummary;
    }

    public void setTemplateSelectionSummary(String templateSelectionSummary) {
        this.templateSelectionSummary = templateSelectionSummary;
    }

    public String getSourceSelectionSummary() {
        return sourceSelectionSummary;
    }

    public void setSourceSelectionSummary(String sourceSelectionSummary) {
        this.sourceSelectionSummary = sourceSelectionSummary;
    }

    public List<String> getMatchedParagraphIds() {
        return matchedParagraphIds;
    }

    public void setMatchedParagraphIds(List<String> matchedParagraphIds) {
        this.matchedParagraphIds = matchedParagraphIds;
    }

    public List<ParagraphNodeDto> getStyledParagraphs() {
        return styledParagraphs;
    }

    public void setStyledParagraphs(List<ParagraphNodeDto> styledParagraphs) {
        this.styledParagraphs = styledParagraphs;
    }
}