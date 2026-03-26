package com.khwajeong.hwpx_saas.compose.dto;

public class ComposeSelectionRequest {
    private String sessionId;
    private String side;
    private Integer page;
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private String label;
    private String ocrText;

    // =========================
    // 추가:
    // 사용자가 직접 선택한 paragraphId
    // OCR 텍스트보다 이 값을 우선 사용하도록 바꿀 예정
    // =========================
    private String paragraphId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
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
}