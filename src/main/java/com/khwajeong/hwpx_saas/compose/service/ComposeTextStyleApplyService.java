package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.StyleProfileDto;
import io.github.mumberrymountain.model.Text;
import io.github.mumberrymountain.model.table.Align;
import io.github.mumberrymountain.render.style.StyleRenderer;
import kr.dogfoot.hwpxlib.object.HWPXFile;
import kr.dogfoot.hwpxlib.reader.HWPXReader;
import kr.dogfoot.hwpxlib.writer.HWPXWriter;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class ComposeTextStyleApplyService {

    public Path applyTextStyleUsingRenderer(
            Path sourceHwpxPath,
            StyleProfileDto templateStyle,
            List<ParagraphNodeDto> sourceParagraphs,
            List<String> matchedParagraphIds
    ) throws Exception {

        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }
        if (templateStyle == null) {
            throw new IllegalArgumentException("templateStyle is required");
        }
        if (sourceParagraphs == null || sourceParagraphs.isEmpty()) {
            throw new IllegalArgumentException("sourceParagraphs is empty");
        }
        if (matchedParagraphIds == null || matchedParagraphIds.isEmpty()) {
            throw new IllegalArgumentException("matchedParagraphIds is empty");
        }

        // 1) source를 HWPX 객체로 열고
        HWPXFile hwpxFile = HWPXReader.fromFilepath(sourceHwpxPath.toString());

        // 2) 예전 프로젝트 방식대로 Text -> StyleRenderer 사용해서 charPr/paraPr 생성
        StyleRenderer styleRenderer = new StyleRenderer(hwpxFile.headerXMLFile());

        Text textStyle = new Text("")
                .fontFamily(nvl(templateStyle.getFontFamily(), "맑은 고딕"))
                .fontSize(nvl(templateStyle.getFontSize(), 12))
                .bold(Boolean.TRUE.equals(templateStyle.getBold()))
                .italic(Boolean.TRUE.equals(templateStyle.getItalic()))
                .underLine(Boolean.TRUE.equals(templateStyle.getUnderline()))
                .fontColor(nvl(templateStyle.getColor(), "#000000"))
                .align(toRendererAlignString(templateStyle.getAlign()))
                .lineSpacingPercent(templateStyle.getLineSpacing())
                .spaceBefore(templateStyle.getSpaceBefore())
                .spaceAfter(templateStyle.getSpaceAfter());

        String newCharPrId = styleRenderer.renderCharStyleAndReturnCharPrId(textStyle);

        // paragraph마다 base paraPrId가 다를 수 있으므로 개별 생성
        Map<Integer, String> paragraphIndexToNewParaPrId = new HashMap<>();
        Set<Integer> targetIndexes = parseParagraphIndexes(matchedParagraphIds);

        for (int idx : targetIndexes) {
            if (idx < 0 || idx >= sourceParagraphs.size()) {
                continue;
            }

            ParagraphNodeDto p = sourceParagraphs.get(idx);

            String baseParaPrId = p.getParaPrIdRef();
            Align alignEnum = toAlignEnum(templateStyle.getAlign());

            String newParaPrId = styleRenderer.renderParaStyleFromBaseAndReturnParaPrId(
                    baseParaPrId,
                    alignEnum,
                    templateStyle.getLineSpacing(),
                    templateStyle.getSpaceBefore(),
                    templateStyle.getSpaceAfter()
            );

            paragraphIndexToNewParaPrId.put(idx, newParaPrId);
        }

        // 3) header에 새 스타일 정의가 추가된 source를 먼저 임시 저장
        Path tempStyledSource = Files.createTempFile("compose-style-base-", ".hwpx");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            HWPXWriter.toStream(hwpxFile, baos);
            Files.write(tempStyledSource, baos.toByteArray());
        }

        // 4) section xml에서 target paragraph의 paraPrIDRef / charPrIDRef만 교체
        Path outputPath = Files.createTempFile("compose-render-output-", ".hwpx");

        int globalParagraphIndex = 0;

        try (
                ZipFile sourceZip = new ZipFile(tempStyledSource.toFile());
                ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))
        ) {
            Map<String, byte[]> rewrittenEntries = new LinkedHashMap<>();

            Enumeration<? extends ZipEntry> entries = sourceZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                byte[] originalBytes;
                try (InputStream in = sourceZip.getInputStream(entry)) {
                    originalBytes = in.readAllBytes();
                }

                byte[] outputBytes = originalBytes;

                if (entryName.startsWith("Contents/section") && entryName.endsWith(".xml")) {
                    SectionRewriteResult result = rewriteSectionXml(
                            originalBytes,
                            globalParagraphIndex,
                            targetIndexes,
                            paragraphIndexToNewParaPrId,
                            newCharPrId
                    );
                    outputBytes = result.bytes;
                    globalParagraphIndex += result.processedParagraphCount;
                }

                rewrittenEntries.put(entryName, outputBytes);
            }

            for (Map.Entry<String, byte[]> e : rewrittenEntries.entrySet()) {
                ZipEntry newEntry = new ZipEntry(e.getKey());
                zos.putNextEntry(newEntry);
                if (e.getValue() != null && e.getValue().length > 0) {
                    zos.write(e.getValue());
                }
                zos.closeEntry();
            }
        }

        return outputPath;
    }

    private Set<Integer> parseParagraphIndexes(List<String> matchedParagraphIds) {
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

        return targetIndexes;
    }

    private SectionRewriteResult rewriteSectionXml(
            byte[] xmlBytes,
            int globalStartIndex,
            Set<Integer> targetIndexes,
            Map<Integer, String> paragraphIndexToNewParaPrId,
            String newCharPrId
    ) throws Exception {

        Document doc = parseXml(xmlBytes);

        NodeList paraList = doc.getElementsByTagName("hp:p");
        int paraCount = paraList.getLength();

        for (int i = 0; i < paraCount; i++) {
            Element p = (Element) paraList.item(i);
            int globalIndex = globalStartIndex + i;

            if (!targetIndexes.contains(globalIndex)) {
                continue;
            }

            String newParaPrId = paragraphIndexToNewParaPrId.get(globalIndex);
            if (newParaPrId != null && !newParaPrId.isBlank()) {
                p.setAttribute("paraPrIDRef", newParaPrId);
            }

            NodeList runList = p.getElementsByTagName("hp:run");
            for (int r = 0; r < runList.getLength(); r++) {
                Node runNode = runList.item(r);
                if (runNode instanceof Element runEl) {
                    runEl.setAttribute("charPrIDRef", newCharPrId);
                }
            }

            NodeList rList = p.getElementsByTagName("hp:r");
            for (int r = 0; r < rList.getLength(); r++) {
                Node rNode = rList.item(r);
                if (rNode instanceof Element rEl) {
                    rEl.setAttribute("charPrIDRef", newCharPrId);
                }
            }
        }

        SectionRewriteResult result = new SectionRewriteResult();
        result.bytes = toBytes(doc);
        result.processedParagraphCount = paraCount;
        return result;
    }

    private Document parseXml(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(xmlBytes)));
    }

    private byte[] toBytes(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();

        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    private Align toAlignEnum(String align) {
        if (align == null) return Align.Left;

        String v = align.trim().toLowerCase();
        return switch (v) {
            case "center" -> Align.Center;
            case "right" -> Align.Right;
            default -> Align.Left;
        };
    }

    private String toRendererAlignString(String align) {
        if (align == null) return "left";

        String v = align.trim().toLowerCase();
        return switch (v) {
            case "center" -> "center";
            case "right" -> "right";
            default -> "left";
        };
    }

    private <T> T nvl(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static class SectionRewriteResult {
        private byte[] bytes;
        private int processedParagraphCount;
    }
}