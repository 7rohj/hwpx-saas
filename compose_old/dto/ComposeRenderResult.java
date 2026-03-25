package com.khwajeong.hwpx_saas.compose.dto;

import com.khwajeong.hwpx_saas.compose.model.StyleMapping;

import java.util.List;

public class ComposeRenderResult {

    private boolean ok;
    private String message;
    private String fileName;
    private String filePath;
    private List<StyleMapping> mappings;

    public static ComposeRenderResult of(
            String message,
            String fileName,
            String filePath,
            List<StyleMapping> mappings
    ) {
        ComposeRenderResult result = new ComposeRenderResult();
        result.ok = true;
        result.message = message;
        result.fileName = fileName;
        result.filePath = filePath;
        result.mappings = mappings;
        return result;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public List<StyleMapping> getMappings() {
        return mappings;
    }
}