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

        String blockType = normalizeBlockType(request.getBlockType());

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

        selection.setParagraphId(request.getParagraphId());
        selection.setBlockType(blockType);
        selection.setTableId(request.getTableId());

        selection.setBlockId(request.getBlockId());
        selection.setRole(request.getRole());
        selection.setSubType(request.getSubType());
        selection.setSourceKind(normalizeSourceKind(request.getSourceKind(), request.getSide()));

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

        response.setParagraphId(selection.getParagraphId());
        response.setBlockType(selection.getBlockType());
        response.setTableId(selection.getTableId());

        response.setBlockId(selection.getBlockId());
        response.setRole(selection.getRole());
        response.setSubType(selection.getSubType());
        response.setSourceKind(selection.getSourceKind());

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

    private String normalizeSourceKind(String sourceKind, String side) {
        if (sourceKind != null && !sourceKind.isBlank()) {
            String v = sourceKind.trim().toLowerCase();
            if ("template".equals(v)) {
                return "template";
            }
            if ("source".equals(v)) {
                return "source";
            }
        }

        if (side == null || side.isBlank()) {
            return "source";
        }

        return "template".equalsIgnoreCase(side) ? "template" : "source";
    }

    private boolean equalsSafe(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    public List<ComposeSelection> findAllBySessionIdAndSideAndLabel(
            String sessionId,
            String side,
            String label
    ) {
        List<ComposeSelection> result = new ArrayList<>();

        for (ComposeSelection selection : selections) {
            if (equalsSafe(sessionId, selection.getSessionId())
                    && equalsSafe(side, selection.getSide())
                    && equalsSafe(label, selection.getLabel())) {
                result.add(selection);
            }
        }

        return result;
    }
}