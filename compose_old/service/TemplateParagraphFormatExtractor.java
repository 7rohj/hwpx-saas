package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParagraphFormatProfile;
import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import com.khwajeong.hwpx_saas.compose.service.TemplateXmlStyleProbe.ParagraphSample;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateParagraphFormatExtractor {

    private final TemplateXmlStyleProbe templateXmlStyleProbe;

    public TemplateParagraphFormatExtractor(TemplateXmlStyleProbe templateXmlStyleProbe) {
        this.templateXmlStyleProbe = templateXmlStyleProbe;
    }

    public Map<TemplateRoleType, ParagraphFormatProfile> extract(Path templatePath) throws Exception {
        List<ParagraphSample> samples = templateXmlStyleProbe.extractParagraphSamples(templatePath);

        Map<TemplateRoleType, ParagraphFormatProfile> result =
                new EnumMap<>(TemplateRoleType.class);

        ParagraphSample chapter = findChapterTitle(samples);
        ParagraphSample section = findSectionTitle(samples);
        ParagraphSample subsection = findSubsectionTitle(samples);
        ParagraphSample body = findBody(samples);

        result.put(TemplateRoleType.CHAPTER_TITLE, toProfile(TemplateRoleType.CHAPTER_TITLE, chapter));
        result.put(TemplateRoleType.SECTION_TITLE, toProfile(TemplateRoleType.SECTION_TITLE, section));
        result.put(TemplateRoleType.SUBSECTION_TITLE, toProfile(TemplateRoleType.SUBSECTION_TITLE, subsection));
        result.put(TemplateRoleType.BODY, toProfile(TemplateRoleType.BODY, body));

        // 당장은 표/캡션 제외
        return result;
    }

    private ParagraphFormatProfile toProfile(TemplateRoleType roleType, ParagraphSample sample) {
        ParagraphFormatProfile profile = new ParagraphFormatProfile()
                .setRoleType(roleType);

        if (sample != null) {
            profile.setSampleText(sample.getText());
            profile.setParaPrId(sample.getParaPrId());
            profile.setCharPrId(sample.getCharPrId());
        }

        return profile;
    }

    private ParagraphSample findChapterTitle(List<ParagraphSample> samples) {
        return samples.stream()
                .filter(s -> s.getText() != null)
                .filter(s -> s.getText().matches("^제\\s*\\d+장.*") || s.getText().matches("^\\d+\\.\\s+.+"))
                .findFirst()
                .orElse(null);
    }

    private ParagraphSample findSectionTitle(List<ParagraphSample> samples) {
        return samples.stream()
                .filter(s -> s.getText() != null)
                .filter(s -> s.getText().matches("^\\d+\\.\\d+\\s+.+"))
                .findFirst()
                .orElse(null);
    }

    private ParagraphSample findSubsectionTitle(List<ParagraphSample> samples) {
        return samples.stream()
                .filter(s -> s.getText() != null)
                .filter(s -> s.getText().matches("^[A-Z]\\d+-.*") || s.getText().length() <= 25)
                .findFirst()
                .orElse(null);
    }

    private ParagraphSample findBody(List<ParagraphSample> samples) {
        return samples.stream()
                .filter(s -> s.getText() != null)
                .filter(s -> s.getText().length() >= 20)
                .findFirst()
                .orElse(null);
    }
}