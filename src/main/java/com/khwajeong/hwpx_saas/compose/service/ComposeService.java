package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeResult;
import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.StyleMapping;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ComposeService {

    private final SourceAnalyzer sourceAnalyzer;
    private final TemplateAnalyzer templateAnalyzer;
    private final StyleMapper styleMapper;

    public ComposeService(
            SourceAnalyzer sourceAnalyzer,
            TemplateAnalyzer templateAnalyzer,
            StyleMapper styleMapper
    ) {
        this.sourceAnalyzer = sourceAnalyzer;
        this.templateAnalyzer = templateAnalyzer;
        this.styleMapper = styleMapper;
    }

    public ComposeResult compose(MultipartFile templateFile, MultipartFile sourceFile, String prompt) {
        List<TemplateStyleRole> roles = templateAnalyzer.analyze(templateFile);
        List<ParsedBlock> blocks = sourceAnalyzer.analyze(sourceFile);
        List<StyleMapping> mappings = styleMapper.map(blocks, roles);

        List<String> detectedRoles = mappings.stream()
                .map(m -> m.getSourceType() + ":" + m.getSourceLevel() + " -> " + m.getTargetRole())
                .collect(Collectors.toList());

        return ComposeResult.success(
                "구조 분석 및 스타일 매핑이 완료되었습니다.",
                detectedRoles
        );
    }
}