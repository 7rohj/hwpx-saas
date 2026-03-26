package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionRequest;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionResponse;
import com.khwajeong.hwpx_saas.compose.dto.ComposeSelectionsResponse;
import com.khwajeong.hwpx_saas.compose.model.ComposeSelection;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ComposeSelectionService {

    private final Map<String, List<ComposeSelection>> store = new ConcurrentHashMap<>();

    public ComposeSelectionResponse save(ComposeSelectionRequest request) {
        validate(request);

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

        store.computeIfAbsent(request.getSessionId(), k -> new ArrayList<>()).add(selection);

        return toResponse(selection);
    }

    public ComposeSelectionsResponse getBySessionId(String sessionId) {
        List<ComposeSelection> selections = store.getOrDefault(sessionId, List.of());

        List<ComposeSelectionResponse> items = new ArrayList<>();
        for (ComposeSelection selection : selections) {
            items.add(toResponse(selection));
        }

        ComposeSelectionsResponse response = new ComposeSelectionsResponse();
        response.setSessionId(sessionId);
        response.setSelections(items);
        return response;
    }

    private void validate(ComposeSelectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is null");
        }
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        if (request.getSide() == null || request.getSide().isBlank()) {
            throw new IllegalArgumentException("side is required");
        }
        if (!"template".equalsIgnoreCase(request.getSide()) &&
            !"source".equalsIgnoreCase(request.getSide())) {
            throw new IllegalArgumentException("side must be template or source");
        }
        if (request.getPage() == null || request.getPage() < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (request.getX() == null || request.getY() == null ||
            request.getWidth() == null || request.getHeight() == null) {
            throw new IllegalArgumentException("x, y, width, height are required");
        }
        if (request.getWidth() <= 0 || request.getHeight() <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        if (request.getLabel() == null || request.getLabel().isBlank()) {
            throw new IllegalArgumentException("label is required");
        }
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
        return response;
    }

    public ComposeSelection findFirstBySessionIdAndSideAndLabel(
            String sessionId,
            String side,
            String label
    ) {
        List<ComposeSelection> selections = store.getOrDefault(sessionId, List.of());

        for (ComposeSelection selection : selections) {
            if (sessionId.equals(selection.getSessionId())
                    && side.equalsIgnoreCase(selection.getSide())
                    && label.equalsIgnoreCase(selection.getLabel())) {
                return selection;
            }
        }

        return null;
    }
}