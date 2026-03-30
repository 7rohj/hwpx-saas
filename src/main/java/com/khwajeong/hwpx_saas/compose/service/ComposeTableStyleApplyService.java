package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.ExtractedTableStyle;
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
import java.util.zip.*;

@Service
public class ComposeTableStyleApplyService {

    public Path applyTableStyle(
            Path sourceHwpxPath,
            String sourceTableId,
            ExtractedTableStyle style
    ) throws Exception {
        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }
        if (sourceTableId == null || sourceTableId.isBlank()) {
            throw new IllegalArgumentException("sourceTableId is required");
        }
        if (style == null) {
            throw new IllegalArgumentException("style is required");
        }

        Path outputPath = Files.createTempFile("compose-table-applied-", ".hwpx");

        try (ZipFile zipFile = new ZipFile(sourceHwpxPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {

            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    zos.putNextEntry(new ZipEntry(entry.getName()));
                    zos.closeEntry();
                    continue;
                }

                byte[] bytes;
                try (InputStream in = zipFile.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }

                // section xml 에서 sourceTableId 대상 표를 찾아 스타일 적용
                if (entry.getName().startsWith("Contents/section") && entry.getName().endsWith(".xml")) {
                    bytes = applyTableStyleToSectionXml(bytes, sourceTableId, style);
                }

                ZipEntry newEntry = new ZipEntry(entry.getName());
                zos.putNextEntry(newEntry);
                zos.write(bytes);
                zos.closeEntry();
            }
        }

        return outputPath;
    }

    private byte[] applyTableStyleToSectionXml(
            byte[] sectionXmlBytes,
            String sourceTableId,
            ExtractedTableStyle style
    ) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        Document doc = factory.newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(sectionXmlBytes)));

        NodeList tblList = doc.getElementsByTagName("hp:tbl");

        // sourceTableId = t0, t1... 이므로 section 안에서 순서대로 매칭
        int tableIndex = 0;

        for (int i = 0; i < tblList.getLength(); i++) {
            Node node = tblList.item(i);
            if (!(node instanceof Element tblEl)) {
                continue;
            }

            String currentTableId = "t" + tableIndex;
            tableIndex++;

            if (!sourceTableId.equals(currentTableId)) {
                continue;
            }

            applyStyleToTable(tblEl, style);
        }

        return documentToBytes(doc);
    }

    private void applyStyleToTable(Element tblEl, ExtractedTableStyle style) {
        List<Element> rows = getElements(tblEl, "hp:tr");

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Element rowEl = rows.get(rowIndex);
            List<Element> cells = getElements(rowEl, "hp:tc");

            boolean isHeaderRow = rowIndex == 0;

            for (Element cellEl : cells) {
                if (isHeaderRow) {
                    applyCellStyle(cellEl,
                            style.getHeaderTextStyle(),
                            style.getHeaderBackgroundColor(),
                            style.getHeaderOuterBorder(),
                            style.getHeaderInnerBorder());
                } else {
                    applyCellStyle(cellEl,
                            style.getBodyTextStyle(),
                            style.getBodyBackgroundColor(),
                            style.getBodyOuterBorder(),
                            style.getBodyInnerBorder());
                }
            }
        }
    }

    private void applyCellStyle(
            Element cellEl,
            ExtractedTableStyle.CellTextStyle textStyle,
            String backgroundColor,
            ExtractedTableStyle.BorderStyle outerBorder,
            ExtractedTableStyle.BorderStyle innerBorder
    ) {
        if (cellEl == null) return;

        // 1) 셀 배경색: cell 속성에 임시 custom attr로 기록
        // 실제 HWPX borderFill 구조를 정밀하게 수정하려면 header xml까지 재작성해야 하므로
        // 이번 단계에서는 section xml 셀에 custom marker를 남김.
        if (backgroundColor != null) {
            cellEl.setAttribute("data-bg-color", backgroundColor);
        }

        // 2) 셀 border style marker
        if (outerBorder != null) {
            if (outerBorder.getColor() != null) {
                cellEl.setAttribute("data-outer-border-color", outerBorder.getColor());
            }
            if (outerBorder.getWidth() != null) {
                cellEl.setAttribute("data-outer-border-width", String.valueOf(outerBorder.getWidth()));
            }
            if (outerBorder.getLineStyle() != null) {
                cellEl.setAttribute("data-outer-border-style", outerBorder.getLineStyle());
            }
        }

        if (innerBorder != null) {
            if (innerBorder.getColor() != null) {
                cellEl.setAttribute("data-inner-border-color", innerBorder.getColor());
            }
            if (innerBorder.getWidth() != null) {
                cellEl.setAttribute("data-inner-border-width", String.valueOf(innerBorder.getWidth()));
            }
            if (innerBorder.getLineStyle() != null) {
                cellEl.setAttribute("data-inner-border-style", innerBorder.getLineStyle());
            }
        }

        // 3) 셀 내부 문단/런 텍스트 스타일 덮어쓰기
        List<Element> paraList = getElements(cellEl, "hp:p");
        for (Element paraEl : paraList) {
            if (textStyle != null && textStyle.getAlign() != null) {
                paraEl.setAttribute("data-align", textStyle.getAlign());
            }

            List<Element> runList = getElements(paraEl, "hp:run");
            for (Element runEl : runList) {
                applyRunStyle(runEl, textStyle);
            }
        }
    }

    private void applyRunStyle(Element runEl, ExtractedTableStyle.CellTextStyle textStyle) {
        if (runEl == null || textStyle == null) return;

        if (textStyle.getFontFamily() != null) {
            runEl.setAttribute("data-font-family", textStyle.getFontFamily());
        }
        if (textStyle.getFontSize() != null) {
            runEl.setAttribute("data-font-size", String.valueOf(textStyle.getFontSize()));
        }
        if (textStyle.getBold() != null) {
            runEl.setAttribute("data-bold", String.valueOf(textStyle.getBold()));
        }
        if (textStyle.getItalic() != null) {
            runEl.setAttribute("data-italic", String.valueOf(textStyle.getItalic()));
        }
        if (textStyle.getUnderline() != null) {
            runEl.setAttribute("data-underline", String.valueOf(textStyle.getUnderline()));
        }
        if (textStyle.getFontColor() != null) {
            runEl.setAttribute("data-font-color", textStyle.getFontColor());
        }
    }

    private List<Element> getElements(Element root, String tagName) {
        List<Element> result = new ArrayList<>();
        if (root == null) return result;

        NodeList list = root.getElementsByTagName(tagName);
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node instanceof Element el) {
                result.add(el);
            }
        }
        return result;
    }

    private byte[] documentToBytes(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }
}