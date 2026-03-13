package com.khwajeong.hwpx_saas.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedFileRepository extends JpaRepository<GeneratedFileEntity, Long> {
    List<GeneratedFileEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}