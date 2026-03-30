package com.khwajeong.hwpx_saas.compose.dto;

public class PageLayoutBlockDto {

    // 추가:
    // paragraph | table
    private String blockType;

    // 추가:
    // paragraph일 때 사용
    private String paragraphId;

    // 추가:
    // table일 때 사용
    private String tableId;

    // 추가:
    // 원본 문서 순서
    private Integer orderIndex;

    // 추가:
    // 실제 배치 좌표
    private Integer page;
    private Double x;
    private Double y;
    private Double width;
    private Double height;

    // 추가:
    // 레이아웃 계산용 메타
    private Integer estimatedFontSize;
    private Boolean headingLike;
    private Integer estimatedRows;
    private Integer estimatedCols;
    private Integer textLength;

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphId) {
        this.paragraphId = paragraphId;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
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

    public Integer getEstimatedFontSize() {
        return estimatedFontSize;
    }

    public void setEstimatedFontSize(Integer estimatedFontSize) {
        this.estimatedFontSize = estimatedFontSize;
    }

    public Boolean getHeadingLike() {
        return headingLike;
    }

    public void setHeadingLike(Boolean headingLike) {
        this.headingLike = headingLike;
    }

    public Integer getEstimatedRows() {
        return estimatedRows;
    }

    public void setEstimatedRows(Integer estimatedRows) {
        this.estimatedRows = estimatedRows;
    }

    public Integer getEstimatedCols() {
        return estimatedCols;
    }

    public void setEstimatedCols(Integer estimatedCols) {
        this.estimatedCols = estimatedCols;
    }

    public Integer getTextLength() {
        return textLength;
    }

    public void setTextLength(Integer textLength) {
        this.textLength = textLength;
    }
}