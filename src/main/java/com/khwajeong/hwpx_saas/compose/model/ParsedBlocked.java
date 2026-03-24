package com.khwajeong.hwpx_saas.compose.model;

public class ParsedBlock {

    public enum BlockType {
        HEADING,
        PARAGRAPH,
        TABLE
    }

    private String id;
    private BlockType type;
    private String text;

    private Integer fontSize;
    private Boolean bold;
    private Integer spaceBefore;
    private Integer spaceAfter;

    private Integer headingScore;
    private Integer levelGuess;

    public ParsedBlock() {
    }

    public ParsedBlock(String id, BlockType type, String text) {
        this.id = id;
        this.type = type;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public ParsedBlock setId(String id) {
        this.id = id;
        return this;
    }

    public BlockType getType() {
        return type;
    }

    public ParsedBlock setType(BlockType type) {
        this.type = type;
        return this;
    }

    public String getText() {
        return text;
    }

    public ParsedBlock setText(String text) {
        this.text = text;
        return this;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public ParsedBlock setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public Boolean getBold() {
        return bold;
    }

    public ParsedBlock setBold(Boolean bold) {
        this.bold = bold;
        return this;
    }

    public Integer getSpaceBefore() {
        return spaceBefore;
    }

    public ParsedBlock setSpaceBefore(Integer spaceBefore) {
        this.spaceBefore = spaceBefore;
        return this;
    }

    public Integer getSpaceAfter() {
        return spaceAfter;
    }

    public ParsedBlock setSpaceAfter(Integer spaceAfter) {
        this.spaceAfter = spaceAfter;
        return this;
    }

    public Integer getHeadingScore() {
        return headingScore;
    }

    public ParsedBlock setHeadingScore(Integer headingScore) {
        this.headingScore = headingScore;
        return this;
    }

    public Integer getLevelGuess() {
        return levelGuess;
    }

    public ParsedBlock setLevelGuess(Integer levelGuess) {
        this.levelGuess = levelGuess;
        return this;
    }
}