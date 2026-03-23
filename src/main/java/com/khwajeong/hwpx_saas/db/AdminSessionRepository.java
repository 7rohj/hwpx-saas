package com.khwajeong.hwpx_saas.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminSessionRepository extends JpaRepository<AdminSessionEntity, Long> {
    Optional<AdminSessionEntity> findByTokenHash(String tokenHash);
}