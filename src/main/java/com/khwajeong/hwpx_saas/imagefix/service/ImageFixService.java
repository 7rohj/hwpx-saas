package com.khwajeong.hwpx_saas.imagefix.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khwajeong.hwpx_saas.imagefix.dto.ImageFixResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ImageFixService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ImageFixService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public ImageFixResponse repair(MultipartFile image, String text) {
        try {
            String apiKey = System.getenv("OPENAI_API_KEY");

            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("OPENAI_API_KEY 환경변수가 없습니다.");
            }

            String contentType = image.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "image/png";
            }

            String base64 = Base64.getEncoder().encodeToString(image.getBytes());
            String dataUrl = "data:" + contentType + ";base64," + base64;

            String prompt = """
                    Remove broken or unreadable Korean text from this selected image area.
                    Restore the background naturally.
                    Do not draw new text into the image.
                    Return only the cleaned background area.
                    """;

            Map<String, Object> body = Map.of(
                    "model", "gpt-image-1",
                    "prompt", prompt,
                    "images", List.of(
                            Map.of("image_url", dataUrl)
                    ),
                    "n", 1,
                    "output_format", "png"
            );

            String raw = restClient.post()
                    .uri("/images/edits")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);

            String outputBase64 = root
                    .path("data")
                    .path(0)
                    .path("b64_json")
                    .asText(null);

            if (outputBase64 == null || outputBase64.isBlank()) {
                throw new IllegalStateException("OpenAI 이미지 응답에서 b64_json을 찾지 못했습니다: " + raw);
            }

            String outputDataUrl = "data:image/png;base64," + outputBase64;

            ImageFixResponse.TextLayer textLayer = new ImageFixResponse.TextLayer(
                    text,
                    20,
                    20,
                    42,
                    "#111111",
                    "Pretendard, Apple SD Gothic Neo, Malgun Gothic, sans-serif"
            );

            return new ImageFixResponse(outputDataUrl, textLayer);

        } catch (Exception e) {
            throw new RuntimeException("이미지 보정 실패: " + e.getMessage(), e);
        }
    }
}