package com.khwajeong.hwpx_saas.service;

import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class StorageService {

  private static final Path STORAGE_ROOT = Path.of("storage");
  private static final Path TEMPLATES_DIR = STORAGE_ROOT.resolve("templates");
  private static final Path FILES_DIR = STORAGE_ROOT.resolve("files");

  public StorageService() {
    try {
      Files.createDirectories(TEMPLATES_DIR);
      Files.createDirectories(FILES_DIR);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Path templatesDir() {
    return TEMPLATES_DIR;
  }

  public Path filesDir() {
    return FILES_DIR;
  }

  /** DB에 저장된 storagePath(문자열)을 Path로 변환 + templatesDir 밖으로 탈출 못 하게 방지 */
  public Path resolveTemplatePath(String storagePath) {
    if (storagePath == null || storagePath.isBlank()) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "template storagePath is empty");
    }
    Path p = Path.of(storagePath).normalize();
    if (!p.startsWith(TEMPLATES_DIR.normalize())) {
      throw new ApiException(ErrorCode.INTERNAL_ERROR, "invalid template storage path", storagePath);
    }
    return p;
  }

  /** files/{fileId} 다운로드용 안전 resolve */
  public Path resolveFilePath(String fileId) {
    if (fileId == null || fileId.isBlank()) {
      throw new ApiException(ErrorCode.BAD_REQUEST, "fileId is required");
    }
    Path p = FILES_DIR.resolve(fileId).normalize();
    if (!p.startsWith(FILES_DIR.normalize())) {
      throw new ApiException(ErrorCode.BAD_REQUEST, "invalid fileId", fileId);
    }
    return p;
  }

  public void ensureExists(Path p, ErrorCode notFoundCode, String msg) {
    if (p == null || !Files.exists(p)) {
      throw new ApiException(notFoundCode, msg);
    }
  }
}