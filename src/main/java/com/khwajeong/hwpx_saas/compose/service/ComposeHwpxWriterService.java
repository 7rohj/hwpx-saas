package com.khwajeong.hwpx_saas.compose.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class ComposeHwpxWriterService {

    public Path writeStyledCopy(
            Path sourceHwpxPath,
            String templateParaPrIdRef,
            String templateCharPrIdRef,
            List<String> matchedParagraphIds
    ) throws Exception {

        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }

        if (matchedParagraphIds == null || matchedParagraphIds.isEmpty()) {
            throw new IllegalArgumentException("matchedParagraphIds is empty");
        }

        // =========================
        // 추가:
        // p0, p1, p8 -> 숫자 index Set 으로 변환
        // =========================
        Set<Integer> targetIndexes = new HashSet<>();
        for (String id : matchedParagraphIds) {
            if (id == null) continue;
            String s = id.trim();
            if (s.startsWith("p")) {
                s = s.substring(1);
            }
            try {
                targetIndexes.add(Integer.parseInt(s));
            } catch (Exception ignored) {
            }
        }

        Path outputPath = Files.createTempFile("compose-writer-output-", ".hwpx");

        int globalParagraphIndex = 0;

        try (
                ZipInputStream zis = new ZipInputStream(Files.newInputStream(sourceHwpxPath));
                ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))
        ) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                byte[] originalBytes = zis.readAllBytes();
                byte[] outputBytes = originalBytes;

                // =========================
                // 추가:
                // section*.xml 만 수정 대상
                // =========================
                if (entryName.startsWith("Contents/section") && entryName.endsWith(".xml")) {
                    SectionRewriteResult result = rewriteSectionXml(
                            originalBytes,
                            globalParagraphIndex,
                            targetIndexes,
                            templateParaPrIdRef,
                            templateCharPrIdRef
                    );
                    outputBytes = result.bytes;
                    globalParagraphIndex += result.processedParagraphCount;
                }

                ZipEntry newEntry = new ZipEntry(entryName);
                zos.putNextEntry(newEntry);
                zos.write(outputBytes);
                zos.closeEntry();
                zis.closeEntry();
            }
        }

        return outputPath;
    }

    private SectionRewriteResult rewriteSectionXml(
            byte[] xmlBytes,
            int globalStartIndex,
            Set<Integer> targetIndexes,
            String templateParaPrIdRef,
            String templateCharPrIdRef
    ) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        Document doc = factory.newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(xmlBytes)));

        NodeList paraList = doc.getElementsByTagName("hp:p");
        int paraCount = paraList.getLength();

        for (int i = 0; i < paraCount; i++) {
            Element p = (Element) paraList.item(i);
            int globalIndex = globalStartIndex + i;

            if (!targetIndexes.contains(globalIndex)) {
                continue;
            }

            // =========================
            // 추가:
            // 템플릿 문단의 paraPrIDRef 복사
            // =========================
            if (templateParaPrIdRef != null && !templateParaPrIdRef.isBlank()) {
                p.setAttribute("paraPrIDRef", templateParaPrIdRef);
            }

            // =========================
            // 추가:
            // 문단 내부 run 들의 charPrIDRef 복사
            // =========================
            NodeList runList = p.getElementsByTagName("hp:run");
            for (int r = 0; r < runList.getLength(); r++) {
                Node runNode = runList.item(r);
                if (runNode instanceof Element runEl) {
                    if (templateCharPrIdRef != null && !templateCharPrIdRef.isBlank()) {
                        runEl.setAttribute("charPrIDRef", templateCharPrIdRef);
                    }
                }
            }
        }

        byte[] outBytes = toBytes(doc);

        SectionRewriteResult result = new SectionRewriteResult();
        result.bytes = outBytes;
        result.processedParagraphCount = paraCount;
        return result;
    }

    private byte[] toBytes(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        // =========================
        // 추가:
        // xml 저장 옵션
        // =========================
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    private static class SectionRewriteResult {
        private byte[] bytes;
        private int processedParagraphCount;
    }
}