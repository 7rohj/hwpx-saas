package com.khwajeong.hwpx_saas.db;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "daily_usage",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_daily_usage_user_date_type",
            columnNames = {"user_email", "usage_date", "usage_type"}
        )
    }
)
public class DailyUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 200)
    private String userEmail;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "usage_type", nullable = false, length = 50)
    private String usageType;

    @Column(nullable = false)
    private int count = 0;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        } else {
            updatedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public String getUsageType() {
        return usageType;
    }

    public int getCount() {
        return count;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}