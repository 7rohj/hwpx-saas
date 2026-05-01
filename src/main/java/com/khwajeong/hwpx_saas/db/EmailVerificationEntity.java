package com.khwajeong.hwpx_saas.db;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "email_verifications")
public class EmailVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime verifiedAt;

    @Column(length = 100)
    private String clientIp;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getCode() { return code; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public boolean isVerified() { return verified; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public String getClientIp() { return clientIp; }

    public void setEmail(String email) { this.email = email; }
    public void setCode(String code) { this.code = code; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}