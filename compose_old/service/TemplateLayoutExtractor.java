package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.LayoutSlot;
import com.khwajeong.hwpx_saas.compose.model.LayoutSlotType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class TemplateLayoutExtractor {

    public List<LayoutSlot> extract(MultipartFile templateFile) {
        // TODO:
        // 실제 템플릿 HWP/HWPX를 읽어서
        // 제목/본문/이미지/표 자리(role)를 추출해야 함.
        // 지금은 1차 뼈대용으로 고정 슬롯 리턴.

        List<LayoutSlot> slots = new ArrayList<>();

        slots.add(new LayoutSlot("slot1", LayoutSlotType.CHAPTER_TITLE, "chapterTitle", "제1장 ㅇㅇㅇㅇ"));
        slots.add(new LayoutSlot("slot2", LayoutSlotType.SECTION_TITLE, "sectionTitle", "1. ㅇㅇㅇㅇㅇ"));
        slots.add(new LayoutSlot("slot3", LayoutSlotType.SUBSECTION_TITLE, "subsectionTitle", "1.1 ㅇㅇㅇㅇ"));
        slots.add(new LayoutSlot("slot4", LayoutSlotType.BODY, "bodyText", "본문 내용"));
        slots.add(new LayoutSlot("slot5", LayoutSlotType.IMAGE, "image1", "이미지 자리"));
        slots.add(new LayoutSlot("slot6", LayoutSlotType.IMAGE_CAPTION, "imageCaption1", "A3-1-2.1-01"));
        slots.add(new LayoutSlot("slot7", LayoutSlotType.TABLE, "table1", "표 자리"));
        slots.add(new LayoutSlot("slot8", LayoutSlotType.TABLE_CAPTION, "tableCaption1", "표 제목"));

        return slots;
    }
}