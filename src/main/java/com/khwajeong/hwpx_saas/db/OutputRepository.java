package com.khwajeong.hwpx_saas.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutputRepository extends JpaRepository<OutputEntity, Long> {
  Optional<OutputEntity> findByFileId(String fileId);
}