package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeDebugResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRenderResult;
import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleSet;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
public class ComposeService {

    private final TemplateAnalyzer templateAnalyzer;
    private final SourceAnalyzer sourceAnalyzer;
    private final StyleMapper styleMapper;
    private final TemplateStyleExtractor templateStyleExtractor;
    private final TemplateClonePlanBuilder templateClonePlanBuilder;
    private final TemplateCloneRenderer templateCloneRenderer;

    public ComposeService(
            TemplateAnalyzer templateAnalyzer,
            SourceAnalyzer sourceAnalyzer,
            StyleMapper styleMapper,
            TemplateStyleExtractor templateStyleExtractor,
            TemplateClonePlanBuilder templateClonePlanBuilder,
            TemplateCloneRenderer templateCloneRenderer
    ) {
        this.templateAnalyzer = templateAnalyzer;
        this.sourceAnalyzer = sourceAnalyzer;
        this.styleMapper = styleMapper;
        this.templateStyleExtractor = templateStyleExtractor;
        this.templateClonePlanBuilder = templateClonePlanBuilder;
        this.templateCloneRenderer = templateCloneRenderer;
    }

    public ComposeDebugResponse composeDebug(
            MultipartFile templateFile,
            MultipartFile sourceFile,
            String prompt
    ) {
        List<TemplateStyleRole> templateRoles = templateAnalyzer.analyze(templateFile);
        List<ParsedBlock> sourceBlocks = sourceAnalyzer.analyze(sourceFile);
        List<StyleMapping> mappings = styleMapper.map(sourceBlocks, templateRoles);

        return ComposeDebugResponse.of(
                "템플릿 역할 추출 / 원본문서 구조 분석 / 스타일 매핑 완료",
                templateRoles,
                sourceBlocks,
                mappings
        );
    }

    public ComposeRenderResult composeRender(
            MultipartFile templateFile,
            MultipartFile sourceFile,
            String prompt
    ) throws Exception {
        TemplateStyleSet styleSet = templateStyleExtractor.extract(templateFile);

        List<TemplateStyleRole> templateRoles = templateAnalyzer.analyze(templateFile);
        List<ParsedBlock> sourceBlocks = sourceAnalyzer.analyze(sourceFile);
        List<StyleMapping> mappings = styleMapper.map(sourceBlocks, templateRoles);

        var plan = templateClonePlanBuilder.build(sourceBlocks, mappings, styleSet);
        Path outputPath = templateCloneRenderer.render(templateFile, plan, styleSet);

        return ComposeRenderResult.of(
                "사용자 템플릿 복제 기반 hwpx 생성 완료",
                outputPath.getFileName().toString(),
                outputPath.toAbsolutePath().toString(),
                mappings
        );
    }
}