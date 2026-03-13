package com.khwajeong.hwpx_saas.db;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "generated_files")
public class GeneratedFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String templateId;

    @Column(nullable = false, unique = true, length = 200)
    private String fileId;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(nullable = false, length = 500)
    private String r2ObjectKey;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS / FAILED

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = "SUCCESS";
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getR2ObjectKey() {
        return r2ObjectKey;
    }

    public Long getSize() {
        return size;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public void setR2ObjectKey(String r2ObjectKey) {
        this.r2ObjectKey = r2ObjectKey;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}