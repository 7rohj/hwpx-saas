package com.khwajeong.hwpx_saas.compose.model;

public enum TemplateRoleType {
    CHAPTER_TITLE,     // 예: 제1장 ㅇㅇㅇㅇ
    SECTION_TITLE,     // 예: 1. ㅇㅇㅇㅇㅇ
    SUBSECTION_TITLE,  // 예: 1.1 / A3-1-2.1-01
    BODY,              // 일반 본문
    BULLET_BODY,       // 예: ⦁ ...
    TABLE_HEADER,      // 표 헤더
    TABLE_BODY,        // 표 본문
    FIGURE_TITLE,      // 그림 제목
    FIGURE_BODY        // 그림 설명
}