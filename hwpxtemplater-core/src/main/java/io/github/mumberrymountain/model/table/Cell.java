package io.github.mumberrymountain.model.table;

import io.github.mumberrymountain.model.Text;

public class Cell {
    private final Text text;

    private String backgroundColor = "#FFFFFF";
    private boolean border = true;
    private String borderColor = "#000000";
    private Align align;

    // merge support
    private int rowSpan = 1;
    private int colSpan = 1;
    private boolean covered = false;

    public Cell(String value) {
        text = new Text(value);
    }

    public Text getText(){
        return text;
    }

    public Cell backgroundColor(String backgroundColor){
        this.backgroundColor = backgroundColor;
        return this;
    }

    public Cell fontColor(String fontColor) {
        text.fontColor(fontColor);
        return this;
    }

    public Cell fontFamily(String fontFamily) {
        text.fontFamily(fontFamily);
        return this;
    }

    public Cell fontSize(int fontSize) {
        text.fontSize(fontSize);
        return this;
    }

    public Cell align(Align align){
        this.align = align;
        return this;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getFontColor() {
        return text.getFontColor();
    }

    public String getFontFamily() {
        return text.getFontFamily();
    }

    public int getFontSize() {
        return text.getFontSize();
    }

    public boolean isBorder(){
        return border;
    }

    public String getBorderColor(){
        return borderColor;
    }

    public Align getAlign(){
        return align;
    }

    // -------------------------
    // merge getters/setters
    // -------------------------
    public int getRowSpan() { return rowSpan; }
    public int getColSpan() { return colSpan; }
    public boolean isCovered() { return covered; }

    public Cell rowSpan(int rowSpan) {
        this.rowSpan = rowSpan;
        return this;
    }

    public Cell colSpan(int colSpan) {
        this.colSpan = colSpan;
        return this;
    }

    public Cell covered(boolean covered) {
        this.covered = covered;
        return this;
    }
}
