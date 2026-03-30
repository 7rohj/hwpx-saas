package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionsResponse;
import com.khwajeong.hwpx_saas.compose.model.ComposeSelection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ComposeSelectionService {

    // 현재는 메모리 저장 방식
    private final List<ComposeSelection> selections = new CopyOnWriteArrayList<>();

    public ComposeSelectionResponse save(ComposeSelectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (request.getSide() == null || request.getSide().isBlank()) {
            throw new IllegalArgumentException("side is required");
        }
        if (request.getLabel() == null || request.getLabel().isBlank()) {
            throw new IllegalArgumentException("label is required");
        }

        // 추가:
        // blockType 기본값 보정
        String blockType = normalizeBlockType(request.getBlockType());

        // 추가:
        // paragraph / table 각각 필수값 검증
        if ("paragraph".equals(blockType)) {
            if (request.getParagraphId() == null || request.getParagraphId().isBlank()) {
                throw new IllegalArgumentException("paragraphId is required when blockType=paragraph");
            }
        }

        if ("table".equals(blockType)) {
            if (request.getTableId() == null || request.getTableId().isBlank()) {
                throw new IllegalArgumentException("tableId is required when blockType=table");
            }
        }

        ComposeSelection selection = new ComposeSelection();
        selection.setSessionId(request.getSessionId());
        selection.setSide(request.getSide());
        selection.setPage(request.getPage());
        selection.setX(request.getX());
        selection.setY(request.getY());
        selection.setWidth(request.getWidth());
        selection.setHeight(request.getHeight());
        selection.setLabel(request.getLabel());
        selection.setOcrText(request.getOcrText());

        // 기존:
        // 문단 ID 저장
        selection.setParagraphId(request.getParagraphId());

        // 추가:
        // blockType / tableId 저장
        selection.setBlockType(blockType);
        selection.setTableId(request.getTableId());

        selections.add(selection);

        return toResponse(selection);
    }

    public ComposeSelectionsResponse getBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        List<ComposeSelectionResponse> result = new ArrayList<>();
        for (ComposeSelection selection : selections) {
            if (sessionId.equals(selection.getSessionId())) {
                result.add(toResponse(selection));
            }
        }

        ComposeSelectionsResponse response = new ComposeSelectionsResponse();
        response.setSessionId(sessionId);
        response.setSelections(result);
        return response;
    }

    public ComposeSelection findFirstBySessionIdAndSideAndLabel(String sessionId, String side, String label) {
        for (ComposeSelection selection : selections) {
            if (equalsSafe(sessionId, selection.getSessionId())
                    && equalsSafe(side, selection.getSide())
                    && equalsSafe(label, selection.getLabel())) {
                return selection;
            }
        }
        return null;
    }

    // 추가:
    // blockType까지 같이 찾고 싶을 때 사용할 수 있는 메서드
    public ComposeSelection findFirstBySessionIdAndSideAndLabelAndBlockType(
            String sessionId,
            String side,
            String label,
            String blockType
    ) {
        for (ComposeSelection selection : selections) {
            if (equalsSafe(sessionId, selection.getSessionId())
                    && equalsSafe(side, selection.getSide())
                    && equalsSafe(label, selection.getLabel())
                    && equalsSafe(normalizeBlockType(blockType), selection.getBlockType())) {
                return selection;
            }
        }
        return null;
    }

    private ComposeSelectionResponse toResponse(ComposeSelection selection) {
        ComposeSelectionResponse response = new ComposeSelectionResponse();
        response.setSessionId(selection.getSessionId());
        response.setSide(selection.getSide());
        response.setPage(selection.getPage());
        response.setX(selection.getX());
        response.setY(selection.getY());
        response.setWidth(selection.getWidth());
        response.setHeight(selection.getHeight());
        response.setLabel(selection.getLabel());
        response.setOcrText(selection.getOcrText());

        // 기존:
        // paragraphId 응답 포함
        response.setParagraphId(selection.getParagraphId());

        // 추가:
        // blockType / tableId 응답 포함
        response.setBlockType(selection.getBlockType());
        response.setTableId(selection.getTableId());

        return response;
    }

    private String normalizeBlockType(String blockType) {
        if (blockType == null || blockType.isBlank()) {
            return "paragraph";
        }

        String v = blockType.trim().toLowerCase();
        if ("table".equals(v)) {
            return "table";
        }
        return "paragraph";
    }

    private boolean equalsSafe(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}