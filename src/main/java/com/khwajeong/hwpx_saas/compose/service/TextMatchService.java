package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TextMatchService {

    public ParagraphNodeDto findBestMatch(List<ParagraphNodeDto> paragraphs, String targetText) {
        if (paragraphs == null || paragraphs.isEmpty()) {
            throw new IllegalArgumentException("paragraphs is empty");
        }

        if (targetText == null || targetText.isBlank()) {
            return paragraphs.get(0);
        }

        ParagraphNodeDto best = null;
        int bestScore = -1;

        for (ParagraphNodeDto p : paragraphs) {
            int score = similarity(safe(p.getText()), safe(targetText));
            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }

        return best != null ? best : paragraphs.get(0);
    }

    private int similarity(String a, String b) {
        int score = 0;

        if (a.equals(b)) score += 100;
        if (a.contains(b)) score += 50;
        if (b.contains(a)) score += 50;

        score += longestCommonSubstring(a, b);

        return score;
    }

    private int longestCommonSubstring(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        int max = 0;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    max = Math.max(max, dp[i][j]);
                }
            }
        }
        return max;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}