package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ParagraphNode;
import org.springframework.stereotype.Component;

@Component
public class Heading1Classifier {

    public boolean isHeading1(ParagraphNode candidate, ParagraphNode sample) {
        return score(candidate, sample) >= 6;
    }

    public int score(ParagraphNode candidate, ParagraphNode sample) {
        int score = 0;

        String text = safe(candidate.getText());
        String sampleText = safe(sample.getText());

        if (text.matches("^\\d+\\..*")) {
            score += 3;
        }

        if (Boolean.TRUE.equals(candidate.getSingleLine())) {
            score += 1;
        }

        int maxAllowedLength = Math.max(sampleText.length() + 10, 30);
        if (text.length() <= maxAllowedLength) {
            score += 1;
        }

        if (sameBoolean(candidate.getBold(), sample.getBold())) {
            score += 2;
        }

        if (near(candidate.getFontSize(), sample.getFontSize(), 2)) {
            score += 2;
        }

        if (near(candidate.getSpaceBefore(), sample.getSpaceBefore(), 4)) {
            score += 1;
        }

        return score;
    }

    private boolean sameBoolean(Boolean a, Boolean b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private boolean near(Integer a, Integer b, int tolerance) {
        if (a == null || b == null) return false;
        return Math.abs(a - b) <= tolerance;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}