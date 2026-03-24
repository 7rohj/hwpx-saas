package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.TemplateClonePlan;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TemplateClonePreviewService {

    public Path writePreview(MultipartFile templateFile, TemplateClonePlan plan) throws Exception {
        Files.createDirectories(Path.of("generated-compose"));

        Path output = Path.of("generated-compose", "clone_plan_" + System.currentTimeMillis() + ".txt");

        StringBuilder sb = new StringBuilder();
        sb.append("TEMPLATE FILE = ").append(templateFile.getOriginalFilename()).append("\n\n");

        for (TemplateClonePlan.CloneBlock block : plan.getBlocks()) {
            sb.append("ROLE = ").append(block.getRoleType()).append("\n");
            sb.append("baseParaPrId = ").append(block.getBaseParaPrId()).append("\n");
            sb.append("baseCharPrId = ").append(block.getBaseCharPrId()).append("\n");
            sb.append("TEXT = ").append(block.getText()).append("\n");
            sb.append("--------------------------------------------------\n");
        }

        Files.writeString(output, sb.toString(), StandardCharsets.UTF_8);
        return output;
    }
}