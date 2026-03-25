package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeDebugResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRenderResult;
import com.khwajeong.hwpx_saas.compose.model.ParagraphFormatProfile;
import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class ComposeService {

    private final TemplateAnalyzer templateAnalyzer;
    private final SourceAnalyzer sourceAnalyzer;
    private final StyleMapper styleMapper;
    private final TemplateParagraphFormatExtractor templateParagraphFormatExtractor;
    private final SourceDocumentStyleApplier sourceDocumentStyleApplier;

    public ComposeService(
            TemplateAnalyzer templateAnalyzer,
            SourceAnalyzer sourceAnalyzer,
            StyleMapper styleMapper,
            TemplateParagraphFormatExtractor templateParagraphFormatExtractor,
            SourceDocumentStyleApplier sourceDocumentStyleApplier
    ) {
        this.templateAnalyzer = templateAnalyzer;
        this.sourceAnalyzer = sourceAnalyzer;
        this.styleMapper = styleMapper;
        this.templateParagraphFormatExtractor = templateParagraphFormatExtractor;
        this.sourceDocumentStyleApplier = sourceDocumentStyleApplier;
    }

    public ComposeDebugResponse composeDebug(
            Path templatePath,
            Path sourcePath,
            String prompt
    ) throws Exception {
        List<TemplateStyleRole> templateRoles = templateAnalyzer.analyze(templatePath);
        List<ParsedBlock> sourceBlocks = sourceAnalyzer.analyze(sourcePath);
        List<StyleMapping> mappings = styleMapper.map(sourceBlocks, templateRoles);

        return ComposeDebugResponse.of(
                "템플릿 역할 추출 / 원본문서 구조 분석 / 스타일 매핑 완료",
                templateRoles,
                sourceBlocks,
                mappings
        );
    }

    public ComposeRenderResult composeRender(
            Path templatePath,
            Path sourcePath,
            String prompt
    ) throws Exception {
        Map<TemplateRoleType, ParagraphFormatProfile> profiles =
                templateParagraphFormatExtractor.extract(templatePath);

        List<TemplateStyleRole> templateRoles = templateAnalyzer.analyze(templatePath);
        List<ParsedBlock> sourceBlocks = sourceAnalyzer.analyze(sourcePath);
        List<StyleMapping> mappings = styleMapper.map(sourceBlocks, templateRoles);

        Path outputPath = sourceDocumentStyleApplier.apply(sourcePath, mappings, profiles);

        return ComposeRenderResult.of(
                "원본문서 문단 서식 덮어쓰기 완료",
                outputPath.getFileName().toString(),
                outputPath.toAbsolutePath().toString(),
                mappings
        );
    }
}