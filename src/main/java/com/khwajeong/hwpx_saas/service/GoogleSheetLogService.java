package com.khwajeong.hwpx_saas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class GoogleSheetLogService {

    private final RestClient restClient = RestClient.create();

    @Value("${app.google-sheet.webhook-url:}")
    private String webhookUrl;

    public void send(String type, String email, String provider, boolean success, String message, Long userId) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("type", type);
            body.put("email", email);
            body.put("provider", provider);
            body.put("success", success);
            body.put("message", message);
            body.put("userId", userId);

            restClient.post()
                    .uri(webhookUrl)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}