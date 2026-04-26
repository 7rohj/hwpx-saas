package com.khwajeong.hwpx_saas.billing.repository;

import com.khwajeong.hwpx_saas.billing.model.UserCredit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCreditRepository extends JpaRepository<UserCredit, String> {
}