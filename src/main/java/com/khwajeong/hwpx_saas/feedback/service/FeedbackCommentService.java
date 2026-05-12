package com.khwajeong.hwpx_saas.feedback.service;

import com.khwajeong.hwpx_saas.feedback.dto.FeedbackCommentRequest;
import com.khwajeong.hwpx_saas.feedback.entity.FeedbackCommentEntity;
import com.khwajeong.hwpx_saas.feedback.repository.FeedbackCommentRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class FeedbackCommentService {

    private final FeedbackCommentRepository repository;

    public FeedbackCommentService(
            FeedbackCommentRepository repository
    ) {
        this.repository = repository;
    }

    public FeedbackCommentEntity create(
            String userEmail,
            FeedbackCommentRequest req
    ) {

        String content = req.getContent().trim();

        if (content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }

        if (content.length() > 500) {
            throw new IllegalArgumentException("댓글은 최대 500자입니다.");
        }

        long count = repository.countByUserEmail(userEmail);

        if (count >= 3) {
            throw new IllegalArgumentException(
                    "한 작성자는 최대 3개의 댓글만 작성 가능합니다."
            );
        }

        FeedbackCommentEntity entity =
                new FeedbackCommentEntity();

        entity.setUserEmail(userEmail);
        entity.setMaskedName(maskEmail(userEmail));
        entity.setContent(content);
        entity.setCreatedAt(OffsetDateTime.now());

        return repository.save(entity);
    }

    public List<FeedbackCommentEntity> getLatest() {
        return repository.findTop20ByOrderByCreatedAtDesc();
    }

    public FeedbackCommentEntity update(
            Long id,
            String userEmail,
            FeedbackCommentRequest req
    ) {

        FeedbackCommentEntity entity =
                repository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException("댓글 없음"));

        if (!entity.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException("본인 댓글만 수정 가능합니다.");
        }

        entity.setContent(req.getContent().trim());
        entity.setUpdatedAt(OffsetDateTime.now());

        return repository.save(entity);
    }

    public void delete(Long id, String userEmail) {

        FeedbackCommentEntity entity =
                repository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException("댓글 없음"));

        if (!entity.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException("본인 댓글만 삭제 가능합니다.");
        }

        repository.delete(entity);
    }

    private String maskEmail(String email) {

        String local =
                email.split("@")[0];

        if (local.length() <= 4) {
            return local + "****";
        }

        return local.substring(0, 4) + "****";
    }
}