package com.khwajeong.hwpx_saas.compose.dto;

import java.util.List;

public class ComposeSelectionsResponse {
    private String sessionId;

    // =========================
    // 유지:
    // 필요하면 현재 선택 중인 paragraphId 하나를 별도로 넣을 수 있음
    // 지금 당장은 selections 리스트가 핵심
    // =========================
    private String paragraphId;

    private List<ComposeSelectionResponse> selections;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    // =========================
    // 추가:
    // paragraphId getter / setter
    // =========================
    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphId) {
        this.paragraphId = paragraphId;
    }

    public List<ComposeSelectionResponse> getSelections() {
        return selections;
    }

    public void setSelections(List<ComposeSelectionResponse> selections) {
        this.selections = selections;
    }
}