package com.khwajeong.hwpx_saas.compose.model;

import java.util.EnumMap;
import java.util.Map;

public class TemplateStyleSet {

    private final Map<TemplateRoleType, ExtractedStyle> styles =
            new EnumMap<>(TemplateRoleType.class);

    public void put(TemplateRoleType roleType, ExtractedStyle style) {
        styles.put(roleType, style);
    }

    public ExtractedStyle get(TemplateRoleType roleType) {
        return styles.get(roleType);
    }

    public Map<TemplateRoleType, ExtractedStyle> getStyles() {
        return styles;
    }
}