package com.khwajeong.hwpx_saas.compose.model;

import java.util.ArrayList;
import java.util.List;

public class TemplateClonePlan {

    private List<CloneBlock> blocks = new ArrayList<>();

    public List<CloneBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<CloneBlock> blocks) {
        this.blocks = blocks;
    }

    public static class CloneBlock {
        private String text;
        private TemplateRoleType roleType;
        private String baseParaPrId;
        private String baseCharPrId;

        public String getText() {
            return text;
        }

        public CloneBlock setText(String text) {
            this.text = text;
            return this;
        }

        public TemplateRoleType getRoleType() {
            return roleType;
        }

        public CloneBlock setRoleType(TemplateRoleType roleType) {
            this.roleType = roleType;
            return this;
        }

        public String getBaseParaPrId() {
            return baseParaPrId;
        }

        public CloneBlock setBaseParaPrId(String baseParaPrId) {
            this.baseParaPrId = baseParaPrId;
            return this;
        }

        public String getBaseCharPrId() {
            return baseCharPrId;
        }

        public CloneBlock setBaseCharPrId(String baseCharPrId) {
            this.baseCharPrId = baseCharPrId;
            return this;
        }
    }
}