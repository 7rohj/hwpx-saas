package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeDebugResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeRenderResult;
import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

@Service
public class ComposeService {

    private final TemplateAnalyzer templateAnalyzer;
    private final SourceAnalyzer sourceAnalyzer;
    private final StyleMapper styleMapper;
    private final RenderedDocumentBuilder renderedDocumentBuilder;

    public ComposeService(
            TemplateAnalyzer templateAnalyzer,
            SourceAnalyzer sourceAnalyzer,
            StyleMapper styleMapper,
            RenderedDocumentBuilder renderedDocumentBuilder
    ) {
        this.templateAnalyzer = templateAnalyzer;
        this.sourceAnalyzer = sourceAnalyzer;
        this.styleMapper = styleMapper;
        this.renderedDocumentBuilder = renderedDocumentBuilder;
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
        List<TemplateStyleRole> templateRoles = templateAnalyzer.analyze(templateFile);
        List<ParsedBlock> sourceBlocks = sourceAnalyzer.analyze(sourceFile);
        List<StyleMapping> mappings = styleMapper.map(sourceBlocks, templateRoles);

        Path outputPath = renderedDocumentBuilder.buildPreviewDocument(sourceBlocks, mappings);

        return ComposeRenderResult.of(
                "렌더링 미리보기 파일 생성 완료",
                outputPath.getFileName().toString(),
                outputPath.toAbsolutePath().toString(),
                mappings
        );
    }
}