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

    // =========================
    // 현재는 메모리 저장 방식
    // 추후 DB로 바꿔도 메서드 시그니처는 유지 가능
    // =========================
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

        // =========================
        // 추가:
        // paragraphId 저장
        // 이제 이 값이 apply의 핵심 기준
        // =========================
        selection.setParagraphId(request.getParagraphId());

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

        // =========================
        // 추가:
        // paragraphId 응답 포함
        // =========================
        response.setParagraphId(selection.getParagraphId());

        return response;
    }

    private boolean equalsSafe(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}