package com.khwajeong.hwpx_saas.compose.dto;

import java.util.List;

public class ComposeRunApplyResponse {
    private String sessionId;
    private String label;
    private String templateSelectionSummary;
    private String sourceSelectionSummary;
    private List<String> matchedParagraphIds;
    private List<ParagraphNodeDto> styledParagraphs;

    // 추가: 생성된 결과 hwpx 파일의 실제 경로
    private String outputPath;

    // 추가: 다운로드용 파일 ID
    private String fileId;

    // 추가: 프론트에서 바로 호출할 다운로드 URL
    private String downloadUrl;

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

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}