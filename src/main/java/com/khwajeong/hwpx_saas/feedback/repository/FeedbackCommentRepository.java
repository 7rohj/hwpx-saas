package com.khwajeong.hwpx_saas.feedback.repository;

import com.khwajeong.hwpx_saas.feedback.entity.FeedbackCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackCommentRepository
        extends JpaRepository<FeedbackCommentEntity, Long> {

    List<FeedbackCommentEntity>
    findTop20ByOrderByCreatedAtDesc();

    long countByUserEmail(String userEmail);
}