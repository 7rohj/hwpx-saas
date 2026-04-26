package com.khwajeong.hwpx_saas.billing.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_credits")
public class UserCredit {

    @Id
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private int balance;

    @Column(nullable = false)
    private String plan;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected UserCredit() {}

    public UserCredit(String email, int balance, String plan) {
        this.email = email;
        this.balance = balance;
        this.plan = plan;
        this.updatedAt = LocalDateTime.now();
    }

    public String getEmail() {
        return email;
    }

    public int getBalance() {
        return balance;
    }

    public String getPlan() {
        return plan;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void deduct(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 코인은 1 이상이어야 합니다.");
        }
        if (balance < amount) {
            throw new IllegalStateException("코인이 부족합니다.");
        }
        this.balance -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void charge(int amount) {
        if (amount <= 0) return;
        this.balance += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void changePlan(String plan) {
        this.plan = plan;
        this.updatedAt = LocalDateTime.now();
    }
}