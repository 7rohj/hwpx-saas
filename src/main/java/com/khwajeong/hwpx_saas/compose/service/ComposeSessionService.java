package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ComposeSessionService {

    public String createSessionId() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "cmp_" + timestamp + "_" + random;
    }
}