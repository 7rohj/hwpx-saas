package com.khwajeong.hwpx_saas;

import com.khwajeong.hwpx_saas.db.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class HwpxSaasApplicationTests {

    // ✅ 핵심: Repository 가짜로 주입
    @MockBean
    private TemplateRepository templateRepository;

    @Test
    void contextLoads() {
    }
}