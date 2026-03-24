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
    private final ComposeHwpxBuilder composeHwpxBuilder;
    private final TemplateStyleExtractor templateStyleExtractor;

    public ComposeService(
            TemplateAnalyzer templateAnalyzer,
            SourceAnalyzer sourceAnalyzer,
            StyleMapper styleMapper,
            ComposeHwpxBuilder composeHwpxBuilder,
            TemplateStyleExtractor templateStyleExtractor
    ) {
        this.templateAnalyzer = templateAnalyzer;
        this.sourceAnalyzer = sourceAnalyzer;
        this.styleMapper = styleMapper;
        this.composeHwpxBuilder = composeHwpxBuilder;
        this.templateStyleExtractor = templateStyleExtractor;
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
        // 1. 템플릿 스타일 추출
        TemplateStyleSet styleSet = templateStyleExtractor.extract(templateFile);

        // 2. 기존 구조 분석
        List<TemplateStyleRole> templateRoles = templateAnalyzer.analyze(templateFile);
        List<ParsedBlock> sourceBlocks = sourceAnalyzer.analyze(sourceFile);
        List<StyleMapping> mappings = styleMapper.map(sourceBlocks, templateRoles);

        // 3. 실제 hwpx 생성
        Path outputPath = composeHwpxBuilder.build(sourceBlocks, mappings, styleSet);

        return ComposeRenderResult.of(
                "스타일 적용 결과 hwpx 생성 완료",
                outputPath.getFileName().toString(),
                outputPath.toAbsolutePath().toString(),
                mappings
        );
    }
}