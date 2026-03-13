package com.khwajeong.hwpx_saas.db;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {
    Optional<TemplateEntity> findByTemplateId(String templateId);
    Optional<TemplateEntity> findByTemplateIdAndUserId(String templateId, Long userId);
    List<TemplateEntity> findByUserIdOrderByUploadedAtDesc(Long userId);
    void deleteByTemplateId(String templateId);
    boolean existsByTemplateId(String templateId);
}