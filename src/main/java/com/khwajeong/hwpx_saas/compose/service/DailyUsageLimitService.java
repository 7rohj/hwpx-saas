package com.khwajeong.hwpx_saas.billing.service;

import com.khwajeong.hwpx_saas.db.DailyUsageEntity;
import com.khwajeong.hwpx_saas.db.DailyUsageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class DailyUsageLimitService {

    private final DailyUsageRepository dailyUsageRepository;

    public DailyUsageLimitService(DailyUsageRepository dailyUsageRepository) {
        this.dailyUsageRepository = dailyUsageRepository;
    }

    @Transactional
    public void checkAndIncrease(String userEmail, String usageType, int dailyLimit) {
        String normalizedUserEmail =
                userEmail == null || userEmail.isBlank()
                        ? "anonymous"
                        : userEmail.trim();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        DailyUsageEntity usage = dailyUsageRepository
                .findByUserEmailAndUsageDateAndUsageType(normalizedUserEmail, today, usageType)
                .orElseGet(() -> {
                    DailyUsageEntity entity = new DailyUsageEntity();
                    entity.setUserEmail(normalizedUserEmail);
                    entity.setUsageDate(today);
                    entity.setUsageType(usageType);
                    entity.setCount(0);
                    return entity;
                });

        if (usage.getCount() >= dailyLimit) {
            throw new IllegalStateException("데모 버전에서는 하루 5회까지만 실행할 수 있습니다.");
        }

        usage.setCount(usage.getCount() + 1);
        dailyUsageRepository.save(usage);
    }
}