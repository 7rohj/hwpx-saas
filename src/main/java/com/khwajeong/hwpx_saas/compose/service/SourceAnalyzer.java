package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParsedBlock;
import com.khwajeong.hwpx_saas.compose.model.ParsedBlock.BlockType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.nio.file.Path;

@Service
public class SourceAnalyzer {

    private static final Pattern CHAPTER_PATTERN =
            Pattern.compile("^\\d+\\.\\s+.+");

    private static final Pattern SECTION_PATTERN =
            Pattern.compile("^\\d+\\.\\d+\\s+.+");

    private static final Pattern CODE_PATTERN =
            Pattern.compile("^[A-Z]\\d+-\\d+-\\d+(\\.\\d+)?-\\d+$");

    // 예: "03 교통수요예측" 같은 페이지 장식/사이드 라벨
    private static final Pattern DECORATION_PATTERN =
            Pattern.compile("^\\d{2}\\s+.+$");

    public List<ParsedBlock> analyze(Path sourceFile) {
        // TODO:
        // 실제 HWP/HWPX 본문 추출 연결 전까지는 샘플 라인으로 검증
        List<String> lines = List.of(
                "2. 사회경제지표 분석",
                "03 교통수요예측",
                "2.1 사회경제지표 분석 기준",
                "A3-1-2.1-01",
                "사회경제지표 정리 및 분석의 기준",
                "사회경제지표 분석은 장래 교통수요예측을 위한 기초자료이다.",
                "2.2 사회경제지표 현황",
                "인구수 / 종사자수 / 학생수",
                "(직접영향권) 연평균 1.78% 증가"
        );

        List<ParsedBlock> result = new ArrayList<>();
        int index = 1;

        for (String line : lines) {
            result.add(classify("b" + index++, line));
        }

        return result;
    }

    private ParsedBlock classify(String id, String text) {
        String trimmed = text == null ? "" : text.trim();

        if (trimmed.isEmpty()) {
            return new ParsedBlock(id, BlockType.PARAGRAPH, trimmed)
                    .setHeadingScore(0)
                    .setLevelGuess(null)
                    .setIgnored(true);
        }

        // 1. 페이지 여백/장식 요소 무시
        // 예: "03 교통수요예측"
        if (DECORATION_PATTERN.matcher(trimmed).matches()) {
            return new ParsedBlock(id, BlockType.HEADING, trimmed)
                    .setHeadingScore(0)
                    .setLevelGuess(null)
                    .setIgnored(true);
        }

        // 2. 절 제목
        // 예: "2.1 사회경제지표 분석 기준"
        if (SECTION_PATTERN.matcher(trimmed).matches()) {
            return new ParsedBlock(id, BlockType.HEADING, trimmed)
                    .setHeadingScore(9)
                    .setLevelGuess(2)
                    .setIgnored(false);
        }

        // 3. 장 제목
        // 예: "2. 사회경제지표 분석"
        if (CHAPTER_PATTERN.matcher(trimmed).matches()) {
            return new ParsedBlock(id, BlockType.HEADING, trimmed)
                    .setHeadingScore(10)
                    .setLevelGuess(1)
                    .setIgnored(false);
        }

        // 4. 코드형 소항목
        // 예: "A3-1-2.1-01"
        if (CODE_PATTERN.matcher(trimmed).matches()) {
            return new ParsedBlock(id, BlockType.CODE, trimmed)
                    .setHeadingScore(7)
                    .setLevelGuess(3)
                    .setIgnored(false);
        }

        // 5. 불릿
        if (trimmed.startsWith("•") || trimmed.startsWith("·") || trimmed.startsWith("⦁")) {
            return new ParsedBlock(id, BlockType.BULLET, trimmed)
                    .setHeadingScore(1)
                    .setLevelGuess(null)
                    .setIgnored(false);
        }

        // 6. 표 헤더 후보
        // 예: "인구수 / 종사자수 / 학생수"
        if (isTableHeaderCandidate(trimmed)) {
            return new ParsedBlock(id, BlockType.TABLE_ROW, trimmed)
                    .setHeadingScore(0)
                    .setLevelGuess(null)
                    .setIgnored(false);
        }

        // 7. 표 본문 후보
        // 예: "(직접영향권) 연평균 1.78% 증가"
        if (isTableBodyCandidate(trimmed)) {
            return new ParsedBlock(id, BlockType.TABLE_ROW, trimmed)
                    .setHeadingScore(0)
                    .setLevelGuess(null)
                    .setIgnored(false);
        }

        // 8. 짧은 제목 후보
        if (trimmed.length() <= 25) {
            return new ParsedBlock(id, BlockType.HEADING, trimmed)
                    .setHeadingScore(6)
                    .setLevelGuess(3)
                    .setIgnored(false);
        }

        // 9. 일반 본문
        return new ParsedBlock(id, BlockType.PARAGRAPH, trimmed)
                .setHeadingScore(0)
                .setLevelGuess(null)
                .setIgnored(false);
    }

    private boolean isTableHeaderCandidate(String text) {
        // 슬래시로 구분되고, 비교적 짧고, 숫자/증가/감소가 없으면 헤더로 판단
        return text.contains("/")
                && text.length() <= 40
                && !text.contains("증가")
                && !text.contains("감소")
                && !containsDigit(text);
    }

    private boolean isTableBodyCandidate(String text) {
        return text.contains("증가")
                || text.contains("감소")
                || containsDigit(text);
    }

    private boolean containsDigit(String text) {
        for (char ch : text.toCharArray()) {
            if (Character.isDigit(ch)) {
                return true;
            }
        }
        return false;
    }
}