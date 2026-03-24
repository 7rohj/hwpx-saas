package com.khwajeong.hwpx_saas.compose.model;

import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole.RoleType;

public class StyleMapping {

    private ParsedBlock.BlockType sourceType;
    private Integer sourceLevel;
    private RoleType targetRole;

    public StyleMapping() {
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

    public RoleType getTargetRole() {
        return targetRole;
    }

    public StyleMapping setTargetRole(RoleType targetRole) {
        this.targetRole = targetRole;
        return this;
    }
}