package com.khwajeong.hwpx_saas.db;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "login_logs")
public class LoginLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false)
    private OffsetDateTime loginAt;

    @Column(nullable = false, length = 30)
    private String provider = "local"; // local / google / kakao / naver

    @Column(length = 100)
    private String message;

    @PrePersist
    public void prePersist() {
        if (loginAt == null) {
            loginAt = OffsetDateTime.now();
        }
        if (provider == null || provider.isBlank()) {
            provider = "local";
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isSuccess() {
        return success;
    }

    public OffsetDateTime getLoginAt() {
        return loginAt;
    }

    public String getProvider() {
        return provider;
    }

    public String getMessage() {
        return message;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setLoginAt(OffsetDateTime loginAt) {
        this.loginAt = loginAt;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}