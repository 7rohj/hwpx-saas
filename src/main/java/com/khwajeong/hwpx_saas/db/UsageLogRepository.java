package com.khwajeong.hwpx_saas.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsageLogRepository extends JpaRepository<UsageLogEntity, Long> {
    List<UsageLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}