package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StyleMapper {

    public List<StyleMapping> map(List<ParsedBlock> blocks, List<TemplateStyleRole> roles) {
        Set<TemplateRoleType> roleSet = roles.stream()
                .map(TemplateStyleRole::getRoleType)
                .collect(Collectors.toSet());

        List<StyleMapping> mappings = new ArrayList<>();

        for (ParsedBlock block : blocks) {
            // 1. 무시 대상은 매핑하지 않음
            if (block.isIgnored()) {
                continue;
            }

            TemplateRoleType targetRole = resolveRole(block, roleSet);
            if (targetRole == null) {
                continue;
            }

            mappings.add(new StyleMapping()
                    .setBlockId(block.getId())
                    .setSourceType(block.getType())
                    .setSourceLevel(block.getLevelGuess())
                    .setTargetRole(targetRole));
        }

        return mappings;
    }

    private TemplateRoleType resolveRole(ParsedBlock block, Set<TemplateRoleType> roleSet) {
        switch (block.getType()) {
            case HEADING:
                if (Integer.valueOf(1).equals(block.getLevelGuess())
                        && roleSet.contains(TemplateRoleType.CHAPTER_TITLE)) {
                    return TemplateRoleType.CHAPTER_TITLE;
                }

                if (Integer.valueOf(2).equals(block.getLevelGuess())
                        && roleSet.contains(TemplateRoleType.SECTION_TITLE)) {
                    return TemplateRoleType.SECTION_TITLE;
                }

                if (Integer.valueOf(3).equals(block.getLevelGuess())
                        && roleSet.contains(TemplateRoleType.SUBSECTION_TITLE)) {
                    return TemplateRoleType.SUBSECTION_TITLE;
                }

                return roleSet.contains(TemplateRoleType.BODY)
                        ? TemplateRoleType.BODY
                        : null;

            case CODE:
                if (roleSet.contains(TemplateRoleType.SUBSECTION_TITLE)) {
                    return TemplateRoleType.SUBSECTION_TITLE;
                }
                return roleSet.contains(TemplateRoleType.BODY)
                        ? TemplateRoleType.BODY
                        : null;

            case BULLET:
                if (roleSet.contains(TemplateRoleType.BULLET_BODY)) {
                    return TemplateRoleType.BULLET_BODY;
                }
                return roleSet.contains(TemplateRoleType.BODY)
                        ? TemplateRoleType.BODY
                        : null;

            case TABLE_ROW:
                if (isTableHeaderLike(block.getText())
                        && roleSet.contains(TemplateRoleType.TABLE_HEADER)) {
                    return TemplateRoleType.TABLE_HEADER;
                }

                if (roleSet.contains(TemplateRoleType.TABLE_BODY)) {
                    return TemplateRoleType.TABLE_BODY;
                }

                return roleSet.contains(TemplateRoleType.BODY)
                        ? TemplateRoleType.BODY
                        : null;

            case TABLE:
                if (roleSet.contains(TemplateRoleType.TABLE_BODY)) {
                    return TemplateRoleType.TABLE_BODY;
                }
                return roleSet.contains(TemplateRoleType.BODY)
                        ? TemplateRoleType.BODY
                        : null;

            case PARAGRAPH:
            default:
                return roleSet.contains(TemplateRoleType.BODY)
                        ? TemplateRoleType.BODY
                        : null;
        }
    }

    private boolean isTableHeaderLike(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String trimmed = text.trim();

        return trimmed.contains("/")
                && trimmed.length() <= 40
                && !trimmed.contains("증가")
                && !trimmed.contains("감소")
                && !containsDigit(trimmed);
    }

    private boolean containsDigit(String text) {
        for (char ch : text.toCharArray()) {
            if (Character.isDigit(ch)) {
                return true;
            }
        }
        return false;
    }
}