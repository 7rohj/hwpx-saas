package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole.RoleType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StyleMapper {

    public List<StyleMapping> map(List<ParsedBlock> blocks, List<TemplateStyleRole> roles) {
        List<StyleMapping> mappings = new ArrayList<>();

        boolean hasTitleL1 = roles.stream().anyMatch(r -> r.getRole() == RoleType.TITLE_L1);
        boolean hasTitleL2 = roles.stream().anyMatch(r -> r.getRole() == RoleType.TITLE_L2);
        boolean hasBody = roles.stream().anyMatch(r -> r.getRole() == RoleType.BODY);

        for (ParsedBlock block : blocks) {
            if (block.getType() == ParsedBlock.BlockType.HEADING) {
                if (Integer.valueOf(1).equals(block.getLevelGuess()) && hasTitleL1) {
                    mappings.add(new StyleMapping()
                            .setSourceType(block.getType())
                            .setSourceLevel(1)
                            .setTargetRole(RoleType.TITLE_L1));
                } else if (Integer.valueOf(2).equals(block.getLevelGuess()) && hasTitleL2) {
                    mappings.add(new StyleMapping()
                            .setSourceType(block.getType())
                            .setSourceLevel(2)
                            .setTargetRole(RoleType.TITLE_L2));
                } else if (hasBody) {
                    mappings.add(new StyleMapping()
                            .setSourceType(block.getType())
                            .setSourceLevel(block.getLevelGuess())
                            .setTargetRole(RoleType.BODY));
                }
            } else if (block.getType() == ParsedBlock.BlockType.PARAGRAPH && hasBody) {
                mappings.add(new StyleMapping()
                        .setSourceType(block.getType())
                        .setSourceLevel(null)
                        .setTargetRole(RoleType.BODY));
            }
        }

        return mappings;
    }
}