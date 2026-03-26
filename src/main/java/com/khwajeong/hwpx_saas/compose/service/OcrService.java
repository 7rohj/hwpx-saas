package com.khwajeong.hwpx_saas.compose.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class OcrService {

    public String read(File imageFile) {
        try {
            ITesseract tesseract = new Tesseract();

            // ⭐ 중요: tessdata 경로
            tesseract.setDatapath("C:\\Users\\User\\AppData\\Local\\Programs\\Tesseract-OCR\\tessdata");

            // ⭐ 언어
            tesseract.setLanguage("kor+eng");

            String text = tesseract.doOCR(imageFile);

            return text.trim();

        } catch (Exception e) {
            throw new RuntimeException("OCR 실패", e);
        }
    }
}