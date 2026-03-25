package com.khwajeong.hwpx_saas.compose.dto;

import java.util.List;

public class ComposeApplyResponse {
    private List<String> matchedParagraphIds;
    private List<ParagraphNodeDto> styledParagraphs;

    public ComposeApplyResponse() {
    }

    public ComposeApplyResponse(List<String> matchedParagraphIds, List<ParagraphNodeDto> styledParagraphs) {
        this.matchedParagraphIds = matchedParagraphIds;
        this.styledParagraphs = styledParagraphs;
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