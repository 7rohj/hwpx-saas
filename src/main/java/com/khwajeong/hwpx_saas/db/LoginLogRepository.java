package com.khwajeong.hwpx_saas.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginLogRepository extends JpaRepository<LoginLogEntity, Long> {
    List<LoginLogEntity> findTop100ByOrderByLoginAtDesc();
}