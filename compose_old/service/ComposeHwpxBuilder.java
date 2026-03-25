package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleSet;
import io.github.mumberrymountain.HWPXTemplater;
import io.github.mumberrymountain.model.Text;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class ComposeHwpxBuilder {

    private final ComposeTextMapper composeTextMapper;

    public ComposeHwpxBuilder(ComposeTextMapper composeTextMapper) {
        this.composeTextMapper = composeTextMapper;
    }

    public Path build(
            List<ParsedBlock> blocks,
            List<StyleMapping> mappings,
            TemplateStyleSet styleSet
    ) throws Exception {

        Files.createDirectories(Path.of("generated-compose"));

        List<Map<String, Object>> items = new ArrayList<>();

        for (ParsedBlock block : blocks) {
            if (block.isIgnored()) {
                continue;
            }

            StyleMapping mapping = mappings.stream()
                    .filter(m -> m.getBlockId().equals(block.getId()))
                    .findFirst()
                    .orElse(null);

            if (mapping == null) {
                continue;
            }

            Text styled = composeTextMapper.map(
                    block,
                    mapping.getTargetRole(),
                    styleSet
            );

            Map<String, Object> item = new HashMap<>();
            item.put("text", styled);
            items.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("items", items);

        String templatePath = "templates/compose-base.hwpx";
        String outputPath = "generated-compose/result_" + System.currentTimeMillis() + ".hwpx";

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            new HWPXTemplater(templatePath)
                    .render(data)
                    .write(fos);
        }

        return Path.of(outputPath);
    }
}