package com.khwajeong.hwpx_saas.db;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyUsageRepository extends JpaRepository<DailyUsageEntity, Long> {

    Optional<DailyUsageEntity> findByUserEmailAndUsageDateAndUsageType(
            String userEmail,
            LocalDate usageDate,
            String usageType
    );
}