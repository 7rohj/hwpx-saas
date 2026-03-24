package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.ParsedBlock.BlockType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class SourceAnalyzer {

    public List<ParsedBlock> analyze(MultipartFile sourceFile) {
        List<ParsedBlock> blocks = new ArrayList<>();

        // TODO: 실제 HWP/HWPX/DOCX/TXT 파싱 연결
        blocks.add(new ParsedBlock("b1", BlockType.HEADING, "샘플 큰 제목")
                .setFontSize(20)
                .setBold(true)
                .setHeadingScore(8)
                .setLevelGuess(1));

        blocks.add(new ParsedBlock("b2", BlockType.HEADING, "샘플 중간 제목")
                .setFontSize(16)
                .setBold(true)
                .setHeadingScore(6)
                .setLevelGuess(2));

        blocks.add(new ParsedBlock("b3", BlockType.PARAGRAPH, "샘플 본문 내용입니다.")
                .setFontSize(11)
                .setBold(false)
                .setHeadingScore(0));

        return blocks;
    }
}