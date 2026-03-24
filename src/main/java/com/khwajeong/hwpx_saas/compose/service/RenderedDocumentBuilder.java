package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class RenderedDocumentBuilder {

    public Path buildPreviewDocument(
            List<ParsedBlock> blocks,
            List<StyleMapping> mappings
    ) throws IOException {

        Path outputDir = Path.of("generated-compose");
        Files.createDirectories(outputDir);

        String fileName = "compose_preview_" + System.currentTimeMillis() + ".txt";
        Path outputPath = outputDir.resolve(fileName);

        StringBuilder sb = new StringBuilder();

        for (ParsedBlock block : blocks) {
            if (block.isIgnored()) {
                continue;
            }

            StyleMapping matched = mappings.stream()
                    .filter(m -> block.getId().equals(m.getBlockId()))
                    .findFirst()
                    .orElse(null);

            TemplateRoleType role = matched != null ? matched.getTargetRole() : null;

            sb.append(renderLine(role, block.getText()));
            sb.append(System.lineSeparator()).append(System.lineSeparator());
        }

        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
        return outputPath;
    }

    private String renderLine(TemplateRoleType role, String text) {
        if (role == null) {
            return text;
        }

        return switch (role) {
            case CHAPTER_TITLE -> "[CHAPTER_TITLE]\n" + text;
            case SECTION_TITLE -> "[SECTION_TITLE]\n" + text;
            case SUBSECTION_TITLE -> "[SUBSECTION_TITLE]\n" + text;
            case BODY -> "[BODY]\n" + text;
            case BULLET_BODY -> "[BULLET_BODY]\n" + text;
            case TABLE_HEADER -> "[TABLE_HEADER]\n" + text;
            case TABLE_BODY -> "[TABLE_BODY]\n" + text;
            case FIGURE_TITLE -> "[FIGURE_TITLE]\n" + text;
            case FIGURE_BODY -> "[FIGURE_BODY]\n" + text;
        };
    }
}