package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ExtractedStyle;
import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleSet;
import com.khwajeong.hwpx_saas.compose.service.TemplateXmlStyleProbe.ParagraphSample;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class TemplateStyleExtractor {

    private final TemplateXmlStyleProbe templateXmlStyleProbe;

    public TemplateStyleExtractor(TemplateXmlStyleProbe templateXmlStyleProbe) {
        this.templateXmlStyleProbe = templateXmlStyleProbe;
    }

    public TemplateStyleSet extract(MultipartFile templateFile) throws Exception {
        Path tempPath = Files.createTempFile("template-style-", ".hwpx");
        templateFile.transferTo(tempPath.toFile());

        try {
            List<ParagraphSample> samples = templateXmlStyleProbe.extractParagraphSamples(tempPath);
            TemplateStyleSet styleSet = new TemplateStyleSet();

            ParagraphSample chapter = findChapterTitle(samples);
            ParagraphSample section = findSectionTitle(samples);
            ParagraphSample subsection = findSubsectionTitle(samples);
            ParagraphSample body = findBody(samples);
            ParagraphSample tableHeader = findTableHeader(samples);
            ParagraphSample tableBody = findTableBody(samples);

            styleSet.put(
                    TemplateRoleType.CHAPTER_TITLE,
                    buildStyle(TemplateRoleType.CHAPTER_TITLE, chapter, 22, true, "left", 20, 10, 160)
            );
            styleSet.put(
                    TemplateRoleType.SECTION_TITLE,
                    buildStyle(TemplateRoleType.SECTION_TITLE, section, 18, true, "left", 12, 6, 160)
            );
            styleSet.put(
                    TemplateRoleType.SUBSECTION_TITLE,
                    buildStyle(TemplateRoleType.SUBSECTION_TITLE, subsection, 14, true, "left", 8, 4, 160)
            );
            styleSet.put(
                    TemplateRoleType.BODY,
                    buildStyle(TemplateRoleType.BODY, body, 11, false, "left", 0, 0, 160)
            );
            styleSet.put(
                    TemplateRoleType.TABLE_HEADER,
                    buildStyle(TemplateRoleType.TABLE_HEADER, tableHeader, 11, true, "center", 0, 0, 160)
            );
            styleSet.put(
                    TemplateRoleType.TABLE_BODY,
                    buildStyle(TemplateRoleType.TABLE_BODY, tableBody, 11, false, "left", 0, 0, 160)
            );

            return styleSet;
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    private ExtractedStyle buildStyle(
            TemplateRoleType roleType,
            ParagraphSample sample,
            int fallbackFontSize,
            boolean fallbackBold,
            String fallbackAlign,
            int fallbackSpaceBefore,
            int fallbackSpaceAfter,
            int fallbackLineSpacing
    ) {
        ExtractedStyle style = new ExtractedStyle()
                .setRoleType(roleType)
                .setFontSize(fallbackFontSize)
                .setBold(fallbackBold)
                .setAlign(fallbackAlign)
                .setSpaceBefore(fallbackSpaceBefore)
                .setSpaceAfter(fallbackSpaceAfter)
                .setLineSpacingPercent(fallbackLineSpacing);

        if (sample != null) {
            style.setSampleText(sample.getText());
            style.setBaseParaPrId(sample.getParaPrId());
            style.setBaseCharPrId(sample.getCharPrId());
            style.setParagraphXml(sample.getParagraphXml());
        }

        return style;
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
                .filter(s -> s.getText().matches("^\\d+\\.\\s+.+"))
                .findFirst()
                .orElse(null);
    }

    private ParagraphSample findSubsectionTitle(List<ParagraphSample> samples) {
        return samples.stream()
                .filter(s -> s.getText() != null)
                .filter(s -> s.getText().matches("^\\d+\\.\\d+.*") || s.getText().matches("^[A-Z]\\d+-.*"))
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

    private ParagraphSample findTableHeader(List<ParagraphSample> samples) {
        return samples.stream()
                .filter(s -> s.getText() != null)
                .filter(s -> s.getText().contains("/") || s.getText().contains("표 헤더"))
                .findFirst()
                .orElse(null);
    }

    private ParagraphSample findTableBody(List<ParagraphSample> samples) {
        return samples.stream()
                .filter(s -> s.getText() != null)
                .filter(s -> s.getText().contains("증가")
                        || s.getText().contains("감소")
                        || s.getText().contains("표 내용"))
                .findFirst()
                .orElse(null);
    }
}