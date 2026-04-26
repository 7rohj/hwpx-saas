package com.khwajeong.hwpx_saas.billing.controller;

import com.khwajeong.hwpx_saas.billing.model.UserCredit;
import com.khwajeong.hwpx_saas.billing.service.CreditService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/credit-test")
public class CreditTestController {

    private final CreditService creditService;

    public CreditTestController(CreditService creditService) {
        this.creditService = creditService;
    }

    @PostMapping("/set-1000")
    public Map<String, Object> set1000() {
        UserCredit credit = creditService.setTestBalance("test@test.com", 1000);

        return Map.of(
                "email", credit.getEmail(),
                "balance", credit.getBalance(),
                "plan", credit.getPlan()
        );
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        UserCredit credit = creditService.getOrCreateFreeUser("test@test.com");

        return Map.of(
                "email", credit.getEmail(),
                "balance", credit.getBalance(),
                "plan", credit.getPlan()
        );
    }
}