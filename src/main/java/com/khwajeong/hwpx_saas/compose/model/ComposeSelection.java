package com.khwajeong.hwpx_saas.compose.model;

public class ComposeSelection {
    private String sessionId;
    private String side;
    private Integer page;
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private String label;
    private String ocrText;

    // 기존:
    // 문단 선택용
    private String paragraphId;

    // 추가:
    // paragraph | table
    private String blockType;

    // 추가:
    // 표 선택용
    private String tableId;

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

    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphId) {
        this.paragraphId = paragraphId;
    }

    // 추가:
    // blockType getter / setter
    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    // 추가:
    // tableId getter / setter
    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }
}