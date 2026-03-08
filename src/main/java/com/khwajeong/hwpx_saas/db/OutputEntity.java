package com.khwajeong.hwpx_saas.db;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "outputs")
public class OutputEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true)
  public String fileId; // 저장된 파일명

  @Column(nullable = false)
  public String outputName;

  @Column(nullable = false)
  public String path; // storage/outputs/...

  @Column(nullable = false)
  public String templateId; // 어떤 템플릿으로 만들었는지(간단히 문자열로)

  @Column(nullable = false)
  public LocalDateTime createdAt = LocalDateTime.now();
}