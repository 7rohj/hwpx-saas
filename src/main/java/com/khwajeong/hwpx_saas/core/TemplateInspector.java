package com.khwajeong.hwpx_saas.core;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class TemplateInspector {

  /**
   * HWPX(zip) 내부의 여러 XML에서 플레이스홀더를 추출합니다.
   * 예: {{title}}, {{ author }}, [[name]], ${value} ...
   *
   * @param hwpxPath .hwpx 경로
   * @param open     시작 구분자 (예: "{{", "[[", "${")
   * @param close    종료 구분자 (예: "}}", "]]", "}")
   */
  public static Set<String> extractPlaceholders(Path hwpxPath, String open, String close) throws Exception {
    if (hwpxPath == null) throw new IllegalArgumentException("hwpxPath is null");
    if (open == null || open.isBlank()) throw new IllegalArgumentException("open delimiter is blank");
    if (close == null || close.isBlank()) throw new IllegalArgumentException("close delimiter is blank");

    // non-greedy + DOTALL로 줄바꿈/태그 사이에도 대응
    String regex = Pattern.quote(open) + "(.*?)" + Pattern.quote(close);
    Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);

    // LinkedHashSet: 중복 제거 + 발견 순서 유지
    Set<String> result = new LinkedHashSet<>();

    try (ZipFile zip = new ZipFile(hwpxPath.toFile())) {
      Enumeration<? extends ZipEntry> entries = zip.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        String name = entry.getName();

        // ✅ 현실적으로 플레이스홀더가 등장 가능한 XML들
        // - Contents/section*.xml : 본문
        // - Contents/header*.xml / footer*.xml : 머리말/꼬리말
        // - Contents/master*.xml : 마스터 페이지
        // - Contents/*.xml 전반(케이스 따라 다름)
        if (!isCandidateXml(name)) continue;

        try (InputStream is = zip.getInputStream(entry)) {
          String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);

          // BOM 제거(﻿) - 파일/툴에 따라 섞여 들어오는 케이스 방지
          xml = stripBom(xml);

          Matcher m = pattern.matcher(xml);
          while (m.find()) {
            String raw = m.group(1);
            if (raw == null) continue;

            // 공백 정리: "{{ title }}" -> "title"
            String key = raw.trim();

            // 빈 키는 무시
            if (!key.isEmpty()) {
              result.add(key);
            }
          }
        }
      }
    }

    return result;
  }

  /**
   * 자동 스캔용(편의): 여러 delimiter를 순서대로 시도해서
   * 첫번째로 발견되는 포맷을 사용하도록 할 때.
   */
  public static Set<String> extractFirstMatchingPlaceholders(Path hwpxPath) throws Exception {
    Set<String> vars = extractPlaceholders(hwpxPath, "{{", "}}");
    if (!vars.isEmpty()) return vars;

    vars = extractPlaceholders(hwpxPath, "[[", "]]");
    if (!vars.isEmpty()) return vars;

    vars = extractPlaceholders(hwpxPath, "${", "}");
    return vars;
    
  }

  private static boolean isCandidateXml(String name) {
    if (name == null) return false;
    if (!name.endsWith(".xml")) return false;

    // 보통 Contents 아래가 핵심이지만, 케이스에 따라 다른 폴더에도 존재 가능
    // 그래도 너무 넓히면 성능 낭비라 현실적인 범위만.
    if (name.startsWith("Contents/")) return true;

    // 안전망: 혹시 Contents가 아닌데도 xml이 들어있는 변종
    // (필요 없으면 주석처리해도 됨)
    return false;
  }

  private static String stripBom(String s) {
    if (s == null || s.isEmpty()) return s;
    // UTF-8 BOM: \uFEFF
    if (s.charAt(0) == '\uFEFF') return s.substring(1);
    return s;
  }
}
