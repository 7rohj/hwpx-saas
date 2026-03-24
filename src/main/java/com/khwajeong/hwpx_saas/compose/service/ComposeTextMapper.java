package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ExtractedStyle;
import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleSet;
import io.github.mumberrymountain.model.Text;
import org.springframework.stereotype.Service;

@Service
public class ComposeTextMapper {

    public Text map(ParsedBlock block, TemplateRoleType role, TemplateStyleSet styleSet) {
        Text t = new Text(block.getText());

        ExtractedStyle style = styleSet.get(role);
        if (style == null) {
            return t.fontSize(11).lineSpacingPercent(160);
        }

        if (style.getFontFamily() != null) t.fontFamily(style.getFontFamily());
        if (style.getFontSize() != null) t.fontSize(style.getFontSize());
        if (style.getBold() != null) t.bold(style.getBold());
        if (style.getFontColor() != null) t.fontColor(style.getFontColor());
        if (style.getAlign() != null) t.align(style.getAlign());
        if (style.getLineSpacingPercent() != null) t.lineSpacingPercent(style.getLineSpacingPercent());
        if (style.getSpaceBefore() != null) t.spaceBefore(style.getSpaceBefore());
        if (style.getSpaceAfter() != null) t.spaceAfter(style.getSpaceAfter());

        return t;
    }
}