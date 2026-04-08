package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Component;

@Component
public class IconClassifier {

    /**
     * 문장 맨 앞의 기호 prefix 추출
     * 예:
     *  "■ 본문입니다." -> "■ "
     *  "※유의사항"     -> "※"
     *  "일반 본문"      -> null
     */
    public String extractPrefix(String text) {
        if (text == null) {
            return null;
        }

        String trimmed = text.stripLeading();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.matches("^[■□●○•※▶▷◆◇☞\\-–—].*")) {
            int firstCharLen = Character.charCount(trimmed.codePointAt(0));
            String first = trimmed.substring(0, firstCharLen);

            if (trimmed.length() > firstCharLen && Character.isWhitespace(trimmed.charAt(firstCharLen))) {
                return first + " ";
            }
            return first;
        }

        return null;
    }

    public boolean isIconText(String text) {
        return extractPrefix(text) != null;
    }
}