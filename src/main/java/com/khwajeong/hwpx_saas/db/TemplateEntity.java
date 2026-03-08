package com.khwajeong.hwpx_saas.db;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import java.time.OffsetDateTime;

@Entity
@Table(name = "templates", indexes = {
    @Index(name = "idx_templates_template_id", columnList = "templateId", unique = true)
})
public class TemplateEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String templateId;

  @Column(nullable = false, length = 255)
  private String originalName;

  @Column(nullable = false, length = 1024)
  private String storagePath;

  @Column(nullable = false)
  private Long sizeBytes;

  @Column(nullable = false)
  private OffsetDateTime uploadedAt;

  @Column(name = "vars_json", columnDefinition = "TEXT")
  private String varsJson;

  protected TemplateEntity() {}

  public TemplateEntity(String templateId, String originalName, String storagePath, Long sizeBytes, OffsetDateTime uploadedAt, String varsJson) {
    this.templateId = templateId;
    this.originalName = originalName;
    this.storagePath = storagePath;
    this.sizeBytes = sizeBytes;
    this.uploadedAt = uploadedAt;
    this.varsJson = varsJson;
  }

  public Long getId() { return id; }
  public String getTemplateId() { return templateId; }
  public String getOriginalName() { return originalName; }
  public String getStoragePath() { return storagePath; }
  public Long getSizeBytes() { return sizeBytes; }
  public OffsetDateTime getUploadedAt() { return uploadedAt; }
  public String getVarsJson() { return varsJson; }

  public void setOriginalName(String originalName) { this.originalName = originalName; }
  public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
  public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
  public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
  public void setVarsJson(String varsJson) { this.varsJson = varsJson; }
}