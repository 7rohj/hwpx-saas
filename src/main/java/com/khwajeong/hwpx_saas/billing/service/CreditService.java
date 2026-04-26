package com.khwajeong.hwpx_saas.billing.service;

import com.khwajeong.hwpx_saas.billing.model.UserCredit;
import com.khwajeong.hwpx_saas.billing.repository.UserCreditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditService {

    private final UserCreditRepository userCreditRepository;

    public CreditService(UserCreditRepository userCreditRepository) {
        this.userCreditRepository = userCreditRepository;
    }

    @Transactional
    public UserCredit getOrCreateFreeUser(String email) {
        return userCreditRepository.findById(email)
                .orElseGet(() -> userCreditRepository.save(
                        new UserCredit(email, 20, "FREE")
                ));
    }

    public int calculateRequiredCredits(int pageCount) {
        if (pageCount <= 10) return 2;
        if (pageCount <= 50) return 5;
        if (pageCount <= 100) return 10;

        throw new IllegalArgumentException("100페이지 초과 문서는 Pro 이상에서 지원됩니다.");
    }

    @Transactional
    public void checkAndDeduct(String email, int pageCount) {
        UserCredit credit = getOrCreateFreeUser(email);

        int required = calculateRequiredCredits(pageCount);

        if ("FREE".equalsIgnoreCase(credit.getPlan()) && pageCount > 10) {
            throw new IllegalStateException("무료 회원은 10페이지 이하 문서만 사용할 수 있습니다.");
        }

        credit.deduct(required);
    }

    @Transactional
    public void refund(String email, int amount) {
        UserCredit credit = getOrCreateFreeUser(email);
        credit.charge(amount);
    }

    @Transactional
    public UserCredit setTestBalance(String email, int balance) {
        UserCredit credit = userCreditRepository.findById(email)
                .orElseGet(() -> userCreditRepository.save(
                        new UserCredit(email, 20, "FREE")
                ));

        int current = credit.getBalance();

        if (balance > current) {
            credit.charge(balance - current);
        } else if (balance < current) {
            credit.deduct(current - balance);
        }

        return credit;
    }
}