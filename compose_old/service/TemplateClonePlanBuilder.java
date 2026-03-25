package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ExtractedStyle;
import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateClonePlan;
import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleSet;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TemplateClonePlanBuilder {

    public TemplateClonePlan build(
            List<ParsedBlock> sourceBlocks,
            List<StyleMapping> mappings,
            TemplateStyleSet styleSet
    ) {
        TemplateClonePlan plan = new TemplateClonePlan();

        for (ParsedBlock block : sourceBlocks) {
            if (block.isIgnored()) continue;

            StyleMapping mapping = mappings.stream()
                    .filter(m -> m.getBlockId().equals(block.getId()))
                    .findFirst()
                    .orElse(null);

            if (mapping == null) continue;

            TemplateRoleType role = mapping.getTargetRole();
            ExtractedStyle style = styleSet.get(role);

            TemplateClonePlan.CloneBlock cloneBlock = new TemplateClonePlan.CloneBlock()
                    .setText(block.getText())
                    .setRoleType(role);

            if (style != null) {
                cloneBlock
                        .setBaseParaPrId(style.getBaseParaPrId())
                        .setBaseCharPrId(style.getBaseCharPrId());
            }

            plan.getBlocks().add(cloneBlock);
        }

        return plan;
    }
}