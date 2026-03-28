package com.khwajeong.hwpx_saas.compose.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Service
public class ComposePageRenderService {

    public byte[] renderPageImage(Path hwpxPath, int page) throws Exception {
        if (hwpxPath == null || !Files.exists(hwpxPath)) {
            throw new IllegalArgumentException("hwpx file not found");
        }

        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }

        Path workDir = Files.createTempDirectory("compose-page-render-");
        Path copiedHwpx = workDir.resolve("input.hwpx");

        try {
            Files.copy(hwpxPath, copiedHwpx);

            System.out.println("=== [PAGE RENDER] hwpxPath = " + hwpxPath + " ===");
            System.out.println("=== [PAGE RENDER] workDir = " + workDir + " ===");
            System.out.println("=== [PAGE RENDER] copiedHwpx = " + copiedHwpx + " ===");

            // LibreOffice headless 변환
            ProcessBuilder pb = new ProcessBuilder(
                    "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
                    "--headless",
                    "--invisible",
                    "--nologo",
                    "--nofirststartwizard",
                    "--convert-to", "pdf",
                    "--outdir", workDir.toAbsolutePath().toString(),
                    copiedHwpx.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            int exitCode = process.waitFor();

            InputStream is = process.getInputStream();
            String output = new String(is.readAllBytes());

            System.out.println("=== [SOFFICE OUTPUT] ===");
            System.out.println(output);
            System.out.println("=== [SOFFICE EXIT CODE] " + exitCode + " ===");

            // 추가:
            // workDir 안의 실제 pdf 파일 탐색
            Path outputPdf = findGeneratedPdf(workDir);

            System.out.println("=== [PAGE RENDER] detected outputPdf = " + outputPdf + " ===");

            if (exitCode != 0 || outputPdf == null || !Files.exists(outputPdf)) {
                throw new RuntimeException("failed to convert hwpx to pdf using soffice");
            }

            try (PDDocument doc = Loader.loadPDF(Files.readAllBytes(outputPdf))) {
                int pageIndex = page - 1;
                if (pageIndex >= doc.getNumberOfPages()) {
                    throw new IllegalArgumentException("requested page exceeds pdf page count");
                }

                PDFRenderer renderer = new PDFRenderer(doc);
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 150);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
            }

        } finally {
            // 디버깅 끝날 때까지 workDir 지우지 말고 싶으면 아래 삭제 주석 처리 가능
            deleteRecursivelyQuietly(workDir);
        }
    }

    // 추가:
    // workDir 안에서 생성된 pdf 파일 찾기
    private Path findGeneratedPdf(Path workDir) {
        try (Stream<Path> stream = Files.list(workDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // 추가:
    // temp 폴더 정리
    private void deleteRecursivelyQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) return;

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}