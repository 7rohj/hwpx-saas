package com.khwajeong.hwpx_saas.compose.dto;

public class TableCellDto {
    private int rowIndex;
    private int colIndex;

    private int rowSpan;
    private int colSpan;

    /**
     * 줄바꿈 포함 셀 텍스트
     */
    private String text;

    /**
     * 표 내부 상대좌표
     * table.x / table.y 를 기준으로 배치되는 상대값
     */
    private Double x;
    private Double y;
    private Double width;
    private Double height;

    /**
     * 기존 프론트 호환용 boolean 값
     * - true  : 헤더 계열 셀
     * - false : 일반 값 셀
     *
     * 이제는 role 이 더 중요한 기준이며,
     * header 는 role 을 단순 boolean 으로 축약한 보조 값입니다.
     */
    private Boolean header;

    /**
     * 셀 의미(role)
     *
     * HEADER_GROUP  : 섹션 그룹 헤더 (예: 기업현황, 대표자, 실무 담당자)
     * HEADER_ROW    : 행 라벨 헤더   (예: 기업명, 홈페이지, 성명, 휴대전화)
     * HEADER_COLUMN : 열/상단 밴드 헤더
     * VALUE         : 실제 값 셀
     */
    private String role;

    /**
     * 셀별 배경색
     */
    private String backgroundColor;

    /**
     * 셀별 텍스트 스타일
     */
    private String fontFamily;
    private Integer fontSize;
    private Boolean bold;
    private Boolean italic;
    private Boolean underline;
    private String fontColor;
    private String align;

    private String borderLeftStyle;
    private String borderLeftColor;
    private String borderLeftWidth;

    private String borderRightStyle;
    private String borderRightColor;
    private String borderRightWidth;

    private String borderTopStyle;
    private String borderTopColor;
    private String borderTopWidth;

    private String borderBottomStyle;
    private String borderBottomColor;
    private String borderBottomWidth;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public Boolean getUnderline() {
        return underline;
    }

    public void setUnderline(Boolean underline) {
        this.underline = underline;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public String getBorderLeftStyle() {
        return borderLeftStyle;
    }

    public void setBorderLeftStyle(String borderLeftStyle) {
        this.borderLeftStyle = borderLeftStyle;
    }

    public String getBorderLeftColor() {
        return borderLeftColor;
    }

    public void setBorderLeftColor(String borderLeftColor) {
        this.borderLeftColor = borderLeftColor;
    }

    public String getBorderLeftWidth() {
        return borderLeftWidth;
    }

    public void setBorderLeftWidth(String borderLeftWidth) {
        this.borderLeftWidth = borderLeftWidth;
    }

    public String getBorderRightStyle() {
        return borderRightStyle;
    }

    public void setBorderRightStyle(String borderRightStyle) {
        this.borderRightStyle = borderRightStyle;
    }

    public String getBorderRightColor() {
        return borderRightColor;
    }

    public void setBorderRightColor(String borderRightColor) {
        this.borderRightColor = borderRightColor;
    }

    public String getBorderRightWidth() {
        return borderRightWidth;
    }

    public void setBorderRightWidth(String borderRightWidth) {
        this.borderRightWidth = borderRightWidth;
    }

    public String getBorderTopStyle() {
        return borderTopStyle;
    }

    public void setBorderTopStyle(String borderTopStyle) {
        this.borderTopStyle = borderTopStyle;
    }

    public String getBorderTopColor() {
        return borderTopColor;
    }

    public void setBorderTopColor(String borderTopColor) {
        this.borderTopColor = borderTopColor;
    }

    public String getBorderTopWidth() {
        return borderTopWidth;
    }

    public void setBorderTopWidth(String borderTopWidth) {
        this.borderTopWidth = borderTopWidth;
    }

    public String getBorderBottomStyle() {
        return borderBottomStyle;
    }

    public void setBorderBottomStyle(String borderBottomStyle) {
        this.borderBottomStyle = borderBottomStyle;
    }

    public String getBorderBottomColor() {
        return borderBottomColor;
    }

    public void setBorderBottomColor(String borderBottomColor) {
        this.borderBottomColor = borderBottomColor;
    }

    public String getBorderBottomWidth() {
        return borderBottomWidth;
    }

    public void setBorderBottomWidth(String borderBottomWidth) {
        this.borderBottomWidth = borderBottomWidth;
    }
}