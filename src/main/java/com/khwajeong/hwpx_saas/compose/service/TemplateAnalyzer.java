package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.TemplateRoleType;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class TemplateAnalyzer {

    public List<TemplateStyleRole> analyze(MultipartFile templateFile) {
        // TODO:
        // 실제 HWP/HWPX 스타일 추출 연결 전까지는
        // 업로드한 템플릿 문서 구조를 기준으로 role set을 고정 샘플로 둔다.
        List<TemplateStyleRole> roles = new ArrayList<>();

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.CHAPTER_TITLE)
                .setSampleText("제1장 ㅇㅇㅇㅇ")
                .setStyleName("chapter-title"));

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.SECTION_TITLE)
                .setSampleText("1. ㅇㅇㅇㅇㅇ")
                .setStyleName("section-title"));

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.SUBSECTION_TITLE)
                .setSampleText("1.1 / A3-1-2.1-01")
                .setStyleName("subsection-title"));

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.BODY)
                .setSampleText("일반 본문")
                .setStyleName("body"));

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.BULLET_BODY)
                .setSampleText("⦁표 내용 스타일 입니다")
                .setStyleName("bullet-body"));

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.TABLE_HEADER)
                .setSampleText("표 헤더")
                .setStyleName("table-header"));

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.TABLE_BODY)
                .setSampleText("표 내용")
                .setStyleName("table-body"));

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.FIGURE_TITLE)
                .setSampleText("그림 제목")
                .setStyleName("figure-title"));

        roles.add(new TemplateStyleRole()
                .setRoleType(TemplateRoleType.FIGURE_BODY)
                .setSampleText("그림 내용")
                .setStyleName("figure-body"));

        return roles;
    }
}