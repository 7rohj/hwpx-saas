package com.khwajeong.hwpx_saas.compose.dto;

public class ComposeRunApplyAllRequest {

    // 추가:
    // session 기준으로 저장된 selection 전체를 가져와
    // 라벨별로 누적 apply 하기 위한 요청 DTO
    private String sessionId;

    // 추가:
    // original | safe
    private String fontPolicy;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getFontPolicy() {
        return fontPolicy;
    }

    public void setFontPolicy(String fontPolicy) {
        this.fontPolicy = fontPolicy;
    }
}