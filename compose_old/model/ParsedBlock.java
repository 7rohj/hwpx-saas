package com.khwajeong.hwpx_saas.compose.model;

public class ParsedBlock {

    public enum BlockType {
        HEADING,
        PARAGRAPH,
        BULLET,
        TABLE,
        TABLE_ROW,
        CODE
    }

    private String id;
    private BlockType type;
    private String text;

    private Integer headingScore;
    private Integer levelGuess;

    private boolean ignored;

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

    public boolean isIgnored() {
        return ignored;
    }

    public ParsedBlock setIgnored(boolean ignored) {
        this.ignored = ignored;
        return this;
    }

    @Override
    public String toString() {
        return "ParsedBlock{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", text='" + text + '\'' +
                ", headingScore=" + headingScore +
                ", levelGuess=" + levelGuess +
                '}';
    }
}