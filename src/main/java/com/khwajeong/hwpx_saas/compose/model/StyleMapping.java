package com.khwajeong.hwpx_saas.compose.model;

public class StyleMapping {

    private String blockId;
    private ParsedBlock.BlockType sourceType;
    private Integer sourceLevel;
    private TemplateRoleType targetRole;

    public String getBlockId() {
        return blockId;
    }

    public StyleMapping setBlockId(String blockId) {
        this.blockId = blockId;
        return this;
    }

    public ParsedBlock.BlockType getSourceType() {
        return sourceType;
    }

    public StyleMapping setSourceType(ParsedBlock.BlockType sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public Integer getSourceLevel() {
        return sourceLevel;
    }

    public StyleMapping setSourceLevel(Integer sourceLevel) {
        this.sourceLevel = sourceLevel;
        return this;
    }

    public TemplateRoleType getTargetRole() {
        return targetRole;
    }

    public StyleMapping setTargetRole(TemplateRoleType targetRole) {
        this.targetRole = targetRole;
        return this;
    }

    @Override
    public String toString() {
        return "StyleMapping{" +
                "blockId='" + blockId + '\'' +
                ", sourceType=" + sourceType +
                ", sourceLevel=" + sourceLevel +
                ", targetRole=" + targetRole +
                '}';
    }
}