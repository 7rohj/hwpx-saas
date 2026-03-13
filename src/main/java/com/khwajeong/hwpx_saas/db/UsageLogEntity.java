package com.khwajeong.hwpx_saas.db;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usage_logs")
public class UsageLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(nullable = false, length = 50)
    private String actionType; // GENERATE, TEMPLATE_UPLOAD 등

    @Column(length = 200)
    private String targetId;

    @Column(nullable = false)
    private Boolean success;

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (success == null) {
            success = Boolean.TRUE;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getTargetId() {
        return targetId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}