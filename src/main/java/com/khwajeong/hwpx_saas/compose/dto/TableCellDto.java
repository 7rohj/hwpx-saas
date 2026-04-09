package com.khwajeong.hwpx_saas.compose.dto;

public class TableCellDto {
    private int rowIndex;
    private int colIndex;

    private int rowSpan;
    private int colSpan;

    private String text;

    /**
     * 표 내부 상대좌표
     * table.x / table.y 를 기준으로 배치되는 상대값
     */
    private Double x;
    private Double y;
    private Double width;
    private Double height;

    private Boolean header;

    /**
     * 셀별 배경색 구분용
     * - 색상 있는 셀 / 없는 셀 구분
     * - preview 에서 header/body 공통색보다 우선 사용
     */
    private String backgroundColor;

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public int getColIndex() {
        return colIndex;
    }

    public void setColIndex(int colIndex) {
        this.colIndex = colIndex;
    }

    public int getRowSpan() {
        return rowSpan;
    }

    public void setRowSpan(int rowSpan) {
        this.rowSpan = rowSpan;
    }

    public int getColSpan() {
        return colSpan;
    }

    public void setColSpan(int colSpan) {
        this.colSpan = colSpan;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public Boolean getHeader() {
        return header;
    }

    public void setHeader(Boolean header) {
        this.header = header;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}