package io.github.mumberrymountain.model;

public class Text {
    private String value;
    private String backgroundColor = "None";
    private String fontColor = "#000000";
    private String fontFamily = "함초롬바탕";
    private int fontSize = 13;
    private boolean bold = false;
    private boolean italic = false;
    private boolean underLine = false;
    private boolean strikeOut = false;
    private boolean outline = false;
    private boolean shadow = false;
    private boolean emboss = false;
    private boolean engrave = false;

    private String align = "null";      // left|center|right|justify
    private String script = "null";   // normal|sup|sub
    private String multiline = "null";  // auto|preserve|none

    private String lineSpacingType = null; // "percent"
    private Integer lineSpacingValue = null; // ex) 160
    private Integer lineSpacingPercent;

    private Integer spaceBefore; // pt
    private Integer spaceAfter;  // pt

    private String baseParaPrId;
    private String baseCharPrId;

    public Text(String value) {
        this.value = value;
    }

    public Text backgroundColor(String backgroundColor){
        this.backgroundColor = backgroundColor;
        return this;
    }

    public Text fontColor(String fontColor) {
        this.fontColor = fontColor;
        return this;
    }

    public Text fontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }

    public Text fontSize(int fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public Text bold(boolean bold){
        this.bold = bold;
        return this;
    }

    public Text italic(boolean italic){
        this.italic = italic;
        return this;
    }

    public Text underLine(boolean underLine){
        this.underLine = underLine;
        return this;
    }

    public Text strikeOut(boolean strikeOut){
        this.strikeOut = strikeOut;
        return this;
    }

    public Text outline(boolean outline){
        this.outline = outline;
        return this;
    }

    public Text shadow(boolean shadow){
        this.shadow = shadow;
        return this;
    }

    public Text emboss(boolean emboss){
        this.emboss = emboss;
        return this;
    }

    public Text engrave(boolean engrave){
        this.engrave = engrave;
        return this;
    }

    // 추가 260219
    public Text align(String align){ 
        this.align = align; 
        return this; 
    }

    public Text script(String script){ 
        this.script = script; 
        return this;  
    }

    public Text multiline(String multiline){
        this.multiline = multiline; 
        return this; 
    }

    // 추가 260225
    public Text lineSpacingType(String t){
         this.lineSpacingType = t; 
         return this; 
    }

    public Text lineSpacingValue(Integer v){ 
        this.lineSpacingValue = v; 
        return this; 
    }

    public Text lineSpacingPercent(Integer percent){
        this.lineSpacingPercent = percent;
        return this;
    }

    // 추가 260227
    public Text spaceBefore(Integer v) {
        this.spaceBefore = v;
        return this;
    }

    public Text spaceAfter(Integer v) {
        this.spaceAfter = v;
        return this;
    }

    // 추가 260325
    public Text baseParaPrId(String baseParaPrId) {
        this.baseParaPrId = baseParaPrId;
        return this;
    }

    public Text baseCharPrId(String baseCharPrId) {
        this.baseCharPrId = baseCharPrId;
        return this;
    }

    public String getBackgroundColor(){
        return backgroundColor;
    }

    public String getFontColor(){
        return fontColor;
    }

    public String getFontFamily(){
        return fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public boolean isBold(){
        return bold;
    }

    public boolean isItalic(){
        return italic;
    }

    public boolean isUnderLine(){
        return underLine;
    }

    public boolean isStrikeOut(){
        return strikeOut;
    }

    public boolean isOutline(){
        return outline;
    }

    public boolean isShadow(){
        return shadow;
    }

    public boolean isEmboss(){
        return emboss;
    }

    public boolean isEngrave(){
        return engrave;
    }

    // 추가 260219
    public String getAlign(){
        return align;
    }

    public String getScript(){
        return script;
    }

    public String getMultiline(){
        return multiline; 
    }

    // 추가 260225
    public String getLineSpacingType(){ 
        return lineSpacingType; 
    }

    public Integer getLineSpacingValue(){ 
        return lineSpacingValue; 
    }

    public Integer getLineSpacingPercent(){
        return lineSpacingPercent;
    }

    // 추가 260227
    public Integer getSpaceBefore() {
        return spaceBefore;
    }

    public Integer getSpaceAfter() {
        return spaceAfter;
    }

    public String getBaseParaPrId() {
        return baseParaPrId;
    }

    public String getBaseCharPrId() {
        return baseCharPrId;
    }

    public String getValue() { return value; }
    public void  setValue(String value) {
        this.value = value;
    }
}
