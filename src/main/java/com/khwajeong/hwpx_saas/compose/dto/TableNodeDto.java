package com.khwajeong.hwpx_saas.compose.dto;

import java.util.List;

public class TableNodeDto {
    private String tableId;
    private Integer page;

    private Integer rowCount;
    private Integer colCount;

    // 셀 텍스트 2차원 배열
    private List<List<String>> cellTexts;

    // 대표 스타일 (MVP)
    private String borderColor;
    private Integer borderWidth;
    private String backgroundColor;

    private String fontFamily;
    private Integer fontSize;
    private Boolean bold;
    private Boolean italic;
    private String fontColor;

    // 임시 좌표
    private Double x;
    private Double y;
    private Double width;
    private Double height;

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Integer getColCount() {
        return colCount;
    }

    public void setColCount(Integer colCount) {
        this.colCount = colCount;
    }

    public List<List<String>> getCellTexts() {
        return cellTexts;
    }

    public void setCellTexts(List<List<String>> cellTexts) {
        this.cellTexts = cellTexts;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    public Integer getBorderWidth() {
        return borderWidth;
    }

    public void setBorderWidth(Integer borderWidth) {
        this.borderWidth = borderWidth;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public Boolean getBold() {
        return bold;
    }

    public void setBold(Boolean bold) {
        this.bold = bold;
    }

    public Boolean getItalic() {
        return italic;
    }

    public void setItalic(Boolean italic) {
        this.italic = italic;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
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
}