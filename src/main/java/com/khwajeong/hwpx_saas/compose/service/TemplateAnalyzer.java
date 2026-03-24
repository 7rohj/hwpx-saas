package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole;
import com.khwajeong.hwpx_saas.compose.model.TemplateStyleRole.RoleType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class TemplateAnalyzer {

    public List<TemplateStyleRole> analyze(MultipartFile templateFile) {
        List<TemplateStyleRole> roles = new ArrayList<>();

        // TODO: 실제 템플릿 파일에서 스타일 추출
        roles.add(new TemplateStyleRole()
                .setRole(RoleType.TITLE_L1)
                .setStyleName("큰 제목")
                .setFontSize(24)
                .setBold(true)
                .setAlign("center")
                .setSpaceAfter(18));

        roles.add(new TemplateStyleRole()
                .setRole(RoleType.TITLE_L2)
                .setStyleName("중간 제목")
                .setFontSize(18)
                .setBold(true)
                .setAlign("left")
                .setSpaceAfter(12));

        roles.add(new TemplateStyleRole()
                .setRole(RoleType.BODY)
                .setStyleName("본문")
                .setFontSize(11)
                .setBold(false)
                .setAlign("justify")
                .setLineSpacing(160));

        return roles;
    }
}