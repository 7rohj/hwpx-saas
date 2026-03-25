package com.khwajeong.hwpx_saas.compose.dto;

import java.util.List;

public class ComposeSelectionsResponse {
    private String sessionId;
    private List<ComposeSelectionResponse> selections;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<ComposeSelectionResponse> getSelections() {
        return selections;
    }

    public void setSelections(List<ComposeSelectionResponse> selections) {
        this.selections = selections;
    }
}