package com.khwajeong.hwpx_saas.feedback.controller;

import com.khwajeong.hwpx_saas.feedback.dto.FeedbackCommentRequest;
import com.khwajeong.hwpx_saas.feedback.entity.FeedbackCommentEntity;
import com.khwajeong.hwpx_saas.feedback.service.FeedbackCommentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback/comments")
public class FeedbackCommentController {

    private final FeedbackCommentService service;

    public FeedbackCommentController(
            FeedbackCommentService service
    ) {
        this.service = service;
    }

    @PostMapping
    public FeedbackCommentEntity create(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody FeedbackCommentRequest req
    ) {
        return service.create(userEmail, req);
    }

    @GetMapping
    public List<FeedbackCommentEntity> getLatest() {
        return service.getLatest();
    }

    @PutMapping("/{id}")
    public FeedbackCommentEntity update(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody FeedbackCommentRequest req
    ) {
        return service.update(id, userEmail, req);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String userEmail
    ) {
        service.delete(id, userEmail);
    }
}