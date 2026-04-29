package com.khwajeong.hwpx_saas.db;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false, length = 30)
    private String role = "USER"; // USER / ADMIN

    @Column(nullable = false, length = 30)
    private String grade = "STARTER"; // STARTER / PRO / BUSINESS

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE / BLOCKED / DELETED
    
    private OffsetDateTime lastLoginAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (role == null || role.isBlank()) {
            role = "USER";
        }
        if (grade == null || grade.isBlank()) {
            grade = "STARTER";
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }        
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public String getGrade() {
        return grade;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}