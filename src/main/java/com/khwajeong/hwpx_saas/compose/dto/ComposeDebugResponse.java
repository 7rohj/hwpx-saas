package com.khwajeong.hwpx_saas.compose.dto;

import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;

import java.util.List;

public class ComposeDebugResponse {

    private boolean ok;
    private String message;
    private List<TemplateStyleRole> templateRoles;
    private List<ParsedBlock> sourceBlocks;
    private List<StyleMapping> mappings;

    public static ComposeDebugResponse of(
            String message,
            List<TemplateStyleRole> templateRoles,
            List<ParsedBlock> sourceBlocks,
            List<StyleMapping> mappings
    ) {
        ComposeDebugResponse response = new ComposeDebugResponse();
        response.ok = true;
        response.message = message;
        response.templateRoles = templateRoles;
        response.sourceBlocks = sourceBlocks;
        response.mappings = mappings;
        return response;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    public List<TemplateStyleRole> getTemplateRoles() {
        return templateRoles;
    }

    public List<ParsedBlock> getSourceBlocks() {
        return sourceBlocks;
    }

    public List<StyleMapping> getMappings() {
        return mappings;
    }
}