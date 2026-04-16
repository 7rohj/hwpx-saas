package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.TableCellDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
import com.khwajeong.hwpx_saas.compose.model.ExtractedTableStyle;
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
public class ComposeTableStyleApplyService {

    private final HwpxStyleRegistryService hwpxStyleRegistryService;

    public ComposeTableStyleApplyService(HwpxStyleRegistryService hwpxStyleRegistryService) {
        this.hwpxStyleRegistryService = hwpxStyleRegistryService;
    }

    /**
     * 기존 일반 table -> table 스타일 적용
     */
    public Path applyTableStyle(
            Path sourceHwpxPath,
            String sourceTableId,
            TableNodeDto templateTable
    ) throws Exception {
        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }
        if (templateTable == null) {
            throw new IllegalArgumentException("templateTable is required");
        }
        if (sourceTableId == null || sourceTableId.isBlank()) {
            throw new IllegalArgumentException("sourceTableId is required");
        }

        int targetTableIndex = parseTargetTableIndex(sourceTableId);
        if (targetTableIndex < 0) {
            throw new IllegalArgumentException("invalid sourceTableId: " + sourceTableId);
        }

        Path outputPath = Files.createTempFile("compose-table-applied-", ".hwpx");

        try (ZipFile zipFile = new ZipFile(sourceHwpxPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {

            Map<String, byte[]> updatedEntries = new LinkedHashMap<>();
            Map<String, Document> headerDocs = loadHeaderDocs(zipFile);

            boolean onelineBox = isOnelineBoxTemplate(templateTable);

            Map<String, RegisteredPair> generalRegisteredByHeader = new LinkedHashMap<>();
            Map<String, Map<String, RegisteredCellStyle>> onelineRegisteredByHeader = new LinkedHashMap<>();

            if (onelineBox) {
                onelineRegisteredByHeader = registerCellStylesToHeaders(headerDocs, templateTable);
            } else {
                ExtractedTableStyle style = templateTable.getExtractedStyle();
                if (style == null) {
                    throw new IllegalArgumentException("templateTable.extractedStyle is required for general table apply");
                }
                generalRegisteredByHeader = registerStylesToHeaders(headerDocs, style);
            }

            int[] currentTableIndexRef = new int[]{0};

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    updatedEntries.put(entry.getName(), new byte[0]);
                    continue;
                }

                byte[] bytes;
                try (InputStream in = zipFile.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }

                if (entry.getName().startsWith("Contents/section") && entry.getName().endsWith(".xml")) {
                    String headerName = toHeaderName(entry.getName());

                    if (onelineBox) {
                        Map<String, RegisteredCellStyle> registeredCellMap = onelineRegisteredByHeader.get(headerName);

                        if (registeredCellMap == null && onelineRegisteredByHeader.containsKey("Contents/header0.xml")) {
                            registeredCellMap = onelineRegisteredByHeader.get("Contents/header0.xml");
                        }
                        if (registeredCellMap == null && onelineRegisteredByHeader.containsKey("Contents/header.xml")) {
                            registeredCellMap = onelineRegisteredByHeader.get("Contents/header.xml");
                        }

                        if (registeredCellMap != null) {
                            bytes = applyOnelineBoxStyleToTargetTableInSectionXml(
                                    bytes,
                                    targetTableIndex,
                                    currentTableIndexRef,
                                    registeredCellMap
                            );
                        }
                    } else {
                        RegisteredPair pair = generalRegisteredByHeader.get(headerName);

                        if (pair == null && generalRegisteredByHeader.containsKey("Contents/header0.xml")) {
                            pair = generalRegisteredByHeader.get("Contents/header0.xml");
                        }
                        if (pair == null && generalRegisteredByHeader.containsKey("Contents/header.xml")) {
                            pair = generalRegisteredByHeader.get("Contents/header.xml");
                        }

                        if (pair != null) {
                            bytes = applyGeneralTableStyleToTargetTableInSectionXml(
                                    bytes,
                                    targetTableIndex,
                                    currentTableIndexRef,
                                    pair
                            );
                        }
                    }
                }

                updatedEntries.put(entry.getName(), bytes);
            }

            for (Map.Entry<String, Document> e : headerDocs.entrySet()) {
                updatedEntries.put(e.getKey(), documentToBytes(e.getValue()));
            }

            for (Map.Entry<String, byte[]> e : updatedEntries.entrySet()) {
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

    /**
     * 신규:
     * template oneline_box(table를 감싼 paragraph 블록 전체)를
     * source paragraph 자리에 삽입하고,
     * source paragraph 텍스트를 슬롯 분해해서 셀에 넣는다.
     */
    public Path applyOnelineBoxToParagraph(
            Path templateHwpxPath,
            String templateTableId,
            Path sourceHwpxPath,
            String sourceParagraphId,
            String sourceParagraphText,
            TableNodeDto templateTable
    ) throws Exception {

        if (templateHwpxPath == null || !Files.exists(templateHwpxPath)) {
            throw new IllegalArgumentException("template hwpx not found");
        }
        if (sourceHwpxPath == null || !Files.exists(sourceHwpxPath)) {
            throw new IllegalArgumentException("source hwpx not found");
        }
        if (templateTableId == null || templateTableId.isBlank()) {
            throw new IllegalArgumentException("templateTableId is required");
        }
        if (sourceParagraphId == null || sourceParagraphId.isBlank()) {
            throw new IllegalArgumentException("sourceParagraphId is required");
        }
        if (templateTable == null) {
            throw new IllegalArgumentException("templateTable is required");
        }

        int templateTableIndex = parseTargetTableIndex(templateTableId);
        int targetParagraphIndex = parseTargetParagraphIndex(sourceParagraphId);

        if (templateTableIndex < 0) {
            throw new IllegalArgumentException("invalid templateTableId: " + templateTableId);
        }
        if (targetParagraphIndex < 0) {
            throw new IllegalArgumentException("invalid sourceParagraphId: " + sourceParagraphId);
        }

        Path outputPath = Files.createTempFile("compose-oneline-box-applied-", ".hwpx");

        try (ZipFile templateZip = new ZipFile(templateHwpxPath.toFile());
             ZipFile sourceZip = new ZipFile(sourceHwpxPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(outputPath))) {

            Map<String, Document> sourceHeaderDocs = loadHeaderDocs(sourceZip);
            Map<String, Map<String, RegisteredCellStyle>> onelineRegisteredByHeader =
                    registerCellStylesToHeaders(sourceHeaderDocs, templateTable);

            TemplateOnelineBoxBlock templateBlock =
                    extractTemplateOnelineBoxBlock(templateZip, templateTableIndex);

            if (templateBlock == null
                    || templateBlock.paragraphElement() == null
                    || templateBlock.tableElement() == null) {
                throw new IllegalArgumentException("template oneline_box block not found: " + templateTableId);
            }

            Map<String, byte[]> updatedEntries = new LinkedHashMap<>();
            int[] currentParagraphIndexRef = new int[]{0};

            Enumeration<? extends ZipEntry> entries = sourceZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                if (entry.isDirectory()) {
                    updatedEntries.put(entry.getName(), new byte[0]);
                    continue;
                }

                byte[] bytes;
                try (InputStream in = sourceZip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }

                if (entry.getName().startsWith("Contents/section") && entry.getName().endsWith(".xml")) {
                    String sourceHeaderName = toHeaderName(entry.getName());

                    Map<String, RegisteredCellStyle> registeredCellMap = onelineRegisteredByHeader.get(sourceHeaderName);
                    if (registeredCellMap == null && onelineRegisteredByHeader.containsKey("Contents/header0.xml")) {
                        registeredCellMap = onelineRegisteredByHeader.get("Contents/header0.xml");
                    }
                    if (registeredCellMap == null && onelineRegisteredByHeader.containsKey("Contents/header.xml")) {
                        registeredCellMap = onelineRegisteredByHeader.get("Contents/header.xml");
                    }

                    bytes = replaceParagraphWithOnelineBoxInSectionXml(
                            bytes,
                            targetParagraphIndex,
                            currentParagraphIndexRef,
                            templateBlock.paragraphElement(),
                            registeredCellMap,
                            sourceParagraphText
                    );
                }

                updatedEntries.put(entry.getName(), bytes);
            }

            for (Map.Entry<String, Document> e : sourceHeaderDocs.entrySet()) {
                updatedEntries.put(e.getKey(), documentToBytes(e.getValue()));
            }

            for (Map.Entry<String, byte[]> e : updatedEntries.entrySet()) {
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

    private Map<String, Document> loadHeaderDocs(ZipFile zipFile) throws Exception {
        Map<String, Document> result = new LinkedHashMap<>();

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            if (!entry.getName().startsWith("Contents/header")) continue;
            if (!entry.getName().endsWith(".xml")) continue;

            try (InputStream in = zipFile.getInputStream(entry)) {
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(in);
                result.put(entry.getName(), doc);
            }
        }

        return result;
    }

    /**
     * 일반 table:
     * 기존 규칙 유지
     * - 헤더/바디 나눠서 일괄 적용
     */
    private Map<String, RegisteredPair> registerStylesToHeaders(
            Map<String, Document> headerDocs,
            ExtractedTableStyle style
    ) {
        Map<String, RegisteredPair> result = new LinkedHashMap<>();

        for (Map.Entry<String, Document> e : headerDocs.entrySet()) {
            Document doc = e.getValue();

            HwpxStyleRegistryService.RegisteredStyleIds headerIds =
                    hwpxStyleRegistryService.registerTableCellStyle(
                            doc,
                            style.getHeaderTextStyle(),
                            style.getHeaderOuterBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderBackgroundColor()
                    );

            HwpxStyleRegistryService.RegisteredStyleIds bodyIds =
                    hwpxStyleRegistryService.registerTableCellStyle(
                            doc,
                            style.getBodyTextStyle(),
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String headerFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getHeaderOuterBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderBackgroundColor()
                    );

            String headerOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getHeaderInnerBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderBackgroundColor()
                    );

            String headerLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getHeaderInnerBorder(),
                            style.getHeaderOuterBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderBackgroundColor()
                    );

            String headerSingleColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getHeaderOuterBorder(),
                            style.getHeaderOuterBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderInnerBorder(),
                            style.getHeaderBackgroundColor()
                    );

            String bodyFirstRowFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyOuterBorder(),
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyFirstRowTopBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyMiddleRowFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyOuterBorder(),
                            style.getBodyInnerVerticalBorder(),
                            hiddenBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyLastRowFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyOuterBorder(),
                            style.getBodyInnerVerticalBorder(),
                            hiddenBorder(),
                            style.getBodyLastRowBottomBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodySingleRowFirstColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyOuterBorder(),
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyFirstRowTopBorder(),
                            style.getBodyLastRowBottomBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyFirstRowOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyFirstRowTopBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyMiddleRowOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyInnerVerticalBorder(),
                            hiddenBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyLastRowOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyInnerVerticalBorder(),
                            hiddenBorder(),
                            style.getBodyLastRowBottomBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodySingleRowOtherColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyFirstRowTopBorder(),
                            style.getBodyLastRowBottomBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyFirstRowLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyOuterBorder(),
                            style.getBodyFirstRowTopBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyMiddleRowLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyOuterBorder(),
                            hiddenBorder(),
                            style.getBodyInnerHorizontalBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodyLastRowLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyOuterBorder(),
                            hiddenBorder(),
                            style.getBodyLastRowBottomBorder(),
                            style.getBodyBackgroundColor()
                    );

            String bodySingleRowLastColBorderFillId =
                    hwpxStyleRegistryService.registerCustomBorderFill(
                            doc,
                            style.getBodyInnerVerticalBorder(),
                            style.getBodyOuterBorder(),
                            style.getBodyFirstRowTopBorder(),
                            style.getBodyLastRowBottomBorder(),
                            style.getBodyBackgroundColor()
                    );

            RegisteredPair pair = new RegisteredPair();
            pair.headerIds = headerIds;
            pair.bodyIds = bodyIds;

            pair.headerFirstColBorderFillId = headerFirstColBorderFillId;
            pair.headerOtherColBorderFillId = headerOtherColBorderFillId;
            pair.headerLastColBorderFillId = headerLastColBorderFillId;
            pair.headerSingleColBorderFillId = headerSingleColBorderFillId;

            pair.bodyFirstRowFirstColBorderFillId = bodyFirstRowFirstColBorderFillId;
            pair.bodyMiddleRowFirstColBorderFillId = bodyMiddleRowFirstColBorderFillId;
            pair.bodyLastRowFirstColBorderFillId = bodyLastRowFirstColBorderFillId;
            pair.bodySingleRowFirstColBorderFillId = bodySingleRowFirstColBorderFillId;

            pair.bodyFirstRowOtherColBorderFillId = bodyFirstRowOtherColBorderFillId;
            pair.bodyMiddleRowOtherColBorderFillId = bodyMiddleRowOtherColBorderFillId;
            pair.bodyLastRowOtherColBorderFillId = bodyLastRowOtherColBorderFillId;
            pair.bodySingleRowOtherColBorderFillId = bodySingleRowOtherColBorderFillId;

            pair.bodyFirstRowLastColBorderFillId = bodyFirstRowLastColBorderFillId;
            pair.bodyMiddleRowLastColBorderFillId = bodyMiddleRowLastColBorderFillId;
            pair.bodyLastRowLastColBorderFillId = bodyLastRowLastColBorderFillId;
            pair.bodySingleRowLastColBorderFillId = bodySingleRowLastColBorderFillId;

            pair.headerFontSize = extractFontSize(style.getHeaderTextStyle(), 12);
            pair.bodyFontSize = extractFontSize(style.getBodyTextStyle(), 10);

            result.put(e.getKey(), pair);
        }

        return result;
    }

    /**
     * oneline_box:
     * 셀별 스타일 등록
     */
    private Map<String, Map<String, RegisteredCellStyle>> registerCellStylesToHeaders(
            Map<String, Document> headerDocs,
            TableNodeDto templateTable
    ) {
        Map<String, Map<String, RegisteredCellStyle>> result = new LinkedHashMap<>();
        Map<String, TableCellDto> templateCellMap = buildTemplateCellMap(templateTable.getCells());

        for (Map.Entry<String, Document> e : headerDocs.entrySet()) {
            Document doc = e.getValue();
            Map<String, RegisteredCellStyle> registeredCellMap = new LinkedHashMap<>();

            for (Map.Entry<String, TableCellDto> cellEntry : templateCellMap.entrySet()) {
                TableCellDto templateCell = cellEntry.getValue();

                ExtractedTableStyle.CellTextStyle textStyle = toCellTextStyle(templateCell);

                HwpxStyleRegistryService.RegisteredStyleIds ids =
                        hwpxStyleRegistryService.registerTableCellStyle(
                                doc,
                                textStyle,
                                hiddenBorder(),
                                hiddenBorder(),
                                templateCell.getBackgroundColor()
                        );

                String borderFillId = hwpxStyleRegistryService.registerCustomBorderFill(
                        doc,
                        toBorderStyle(
                                templateCell.getBorderLeftStyle(),
                                templateCell.getBorderLeftColor(),
                                templateCell.getBorderLeftWidth()
                        ),
                        toBorderStyle(
                                templateCell.getBorderRightStyle(),
                                templateCell.getBorderRightColor(),
                                templateCell.getBorderRightWidth()
                        ),
                        toBorderStyle(
                                templateCell.getBorderTopStyle(),
                                templateCell.getBorderTopColor(),
                                templateCell.getBorderTopWidth()
                        ),
                        toBorderStyle(
                                templateCell.getBorderBottomStyle(),
                                templateCell.getBorderBottomColor(),
                                templateCell.getBorderBottomWidth()
                        ),
                        templateCell.getBackgroundColor()
                );

                RegisteredCellStyle registered = new RegisteredCellStyle();
                registered.ids = ids;
                registered.borderFillId = borderFillId;
                registered.fontSize = templateCell.getFontSize() != null && templateCell.getFontSize() > 0
                        ? templateCell.getFontSize()
                        : 10;

                registeredCellMap.put(cellEntry.getKey(), registered);
            }

            result.put(e.getKey(), registeredCellMap);
        }

        return result;
    }

    private ExtractedTableStyle.BorderStyle hiddenBorder() {
        ExtractedTableStyle.BorderStyle border = new ExtractedTableStyle.BorderStyle();
        border.setLineStyle("NONE");
        border.setWidth("0.1 mm");
        border.setColor("#000000");
        return border;
    }

    private int extractFontSize(ExtractedTableStyle.CellTextStyle textStyle, int defaultValue) {
        if (textStyle == null || textStyle.getFontSize() == null || textStyle.getFontSize() <= 0) {
            return defaultValue;
        }
        return textStyle.getFontSize();
    }

    private ExtractedTableStyle.BorderStyle toBorderStyle(String lineStyle, String color, String width) {
        ExtractedTableStyle.BorderStyle border = new ExtractedTableStyle.BorderStyle();
        border.setLineStyle(
                lineStyle != null && !lineStyle.isBlank()
                        ? lineStyle
                        : "NONE"
        );
        border.setColor(
                color != null && !color.isBlank()
                        ? color
                        : "#000000"
        );
        border.setWidth(
                width != null && !width.isBlank()
                        ? width
                        : "0.12 mm"
        );
        return border;
    }

    private ExtractedTableStyle.CellTextStyle toCellTextStyle(TableCellDto templateCell) {
        ExtractedTableStyle.CellTextStyle textStyle = new ExtractedTableStyle.CellTextStyle();
        textStyle.setFontFamily(templateCell.getFontFamily());
        textStyle.setFontSize(templateCell.getFontSize());
        textStyle.setBold(templateCell.getBold());
        textStyle.setItalic(templateCell.getItalic());
        textStyle.setUnderline(templateCell.getUnderline());
        textStyle.setFontColor(templateCell.getFontColor());
        textStyle.setAlign(templateCell.getAlign());
        return textStyle;
    }

    private Map<String, TableCellDto> buildTemplateCellMap(List<TableCellDto> cells) {
        Map<String, TableCellDto> map = new LinkedHashMap<>();
        if (cells == null) {
            return map;
        }

        for (TableCellDto cell : cells) {
            map.put(cell.getRowIndex() + ":" + cell.getColIndex(), cell);
        }

        return map;
    }

    /**
     * 일반 table:
     * target table 하나에만 기존 헤더/바디 규칙 적용
     */
    private byte[] applyGeneralTableStyleToTargetTableInSectionXml(
            byte[] sectionXmlBytes,
            int targetTableIndex,
            int[] currentTableIndexRef,
            RegisteredPair pair
    ) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(sectionXmlBytes)));

        NodeList tblList = doc.getElementsByTagName("hp:tbl");

        for (int i = 0; i < tblList.getLength(); i++) {
            Node node = tblList.item(i);
            if (!(node instanceof Element tblEl)) continue;

            int currentIndex = currentTableIndexRef[0];
            if (currentIndex == targetTableIndex) {
                applyStyleToGeneralTable(tblEl, pair);
            }

            currentTableIndexRef[0] = currentIndex + 1;
        }

        return documentToBytes(doc);
    }

    /**
     * oneline_box:
     * target table 하나에만 셀별 스타일 적용
     */
    private byte[] applyOnelineBoxStyleToTargetTableInSectionXml(
            byte[] sectionXmlBytes,
            int targetTableIndex,
            int[] currentTableIndexRef,
            Map<String, RegisteredCellStyle> registeredCellMap
    ) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(sectionXmlBytes)));

        NodeList tblList = doc.getElementsByTagName("hp:tbl");

        for (int i = 0; i < tblList.getLength(); i++) {
            Node node = tblList.item(i);
            if (!(node instanceof Element tblEl)) {
                continue;
            }

            int currentIndex = currentTableIndexRef[0];
            if (currentIndex == targetTableIndex) {
                applyStyleToOnelineBoxTable(tblEl, registeredCellMap);
            }

            currentTableIndexRef[0] = currentIndex + 1;
        }

        return documentToBytes(doc);
    }

    /**
     * source section xml 안에서 target paragraph를 찾아
     * template oneline_box paragraph block으로 교체한다.
     */
    private byte[] replaceParagraphWithOnelineBoxInSectionXml(
            byte[] sectionXmlBytes,
            int targetParagraphIndex,
            int[] currentParagraphIndexRef,
            Element templateParagraphElement,
            Map<String, RegisteredCellStyle> registeredCellMap,
            String sourceParagraphText
    ) throws Exception {

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(new ByteArrayInputStream(sectionXmlBytes)));

        if (templateParagraphElement == null) {
            return documentToBytes(doc);
        }

        boolean replaced = replaceParagraphRecursive(
                doc.getDocumentElement(),
                doc,
                targetParagraphIndex,
                currentParagraphIndexRef,
                templateParagraphElement,
                registeredCellMap,
                sourceParagraphText
        );

        if (!replaced) {
            System.out.println("[oneline_box] target paragraph not found in this section, currentParagraphIndex=" + currentParagraphIndexRef[0]);
        }

        return documentToBytes(doc);
    }

    /**
     * 재귀적으로 paragraph를 찾아 교체
     */
    private boolean replaceParagraphRecursive(
            Element current,
            Document ownerDoc,
            int targetParagraphIndex,
            int[] currentParagraphIndexRef,
            Element templateParagraphElement,
            Map<String, RegisteredCellStyle> registeredCellMap,
            String sourceParagraphText
    ) {
        if (current == null) {
            return false;
        }

        NodeList children = current.getChildNodes();
        List<Node> childSnapshot = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            childSnapshot.add(children.item(i));
        }

        for (Node child : childSnapshot) {
            if (!(child instanceof Element childEl)) {
                continue;
            }

            if (isParagraphElement(childEl)) {
                int currentIndex = currentParagraphIndexRef[0];
                System.out.println("[oneline_box] visiting paragraph index = " + currentIndex);

                if (currentIndex == targetParagraphIndex) {
                    System.out.println("[oneline_box] REPLACE paragraph index = "
                            + currentIndex + ", sourceText = " + sourceParagraphText);

                    Element importedParagraph = (Element) ownerDoc.importNode(templateParagraphElement, true);

                    Element importedTable = findFirstDescendant(importedParagraph, "hp:tbl");
                    if (importedTable == null) {
                        System.out.println("[oneline_box] imported paragraph has no hp:tbl");
                        return false;
                    }

                    if (registeredCellMap != null) {
                        applyStyleToOnelineBoxTable(importedTable, registeredCellMap);
                    }

                    injectSourceTextIntoOnelineBox(importedTable, sourceParagraphText);

                    current.replaceChild(importedParagraph, childEl);
                    return true;
                }

                currentParagraphIndexRef[0] = currentIndex + 1;
            }

            if (replaceParagraphRecursive(
                    childEl,
                    ownerDoc,
                    targetParagraphIndex,
                    currentParagraphIndexRef,
                    templateParagraphElement,
                    registeredCellMap,
                    sourceParagraphText
            )) {
                return true;
            }
        }

        return false;
    }

    /**
     * 일반 table: 기존 헤더/바디 bulk 규칙 유지
     */
    private void applyStyleToGeneralTable(Element tblEl, RegisteredPair pair) {
        List<Element> rows = getElements(tblEl, "hp:tr");
        int rowCount = rows.size();
        int lastRowIndex = rowCount - 1;

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Element rowEl = rows.get(rowIndex);
            List<Element> cells = getElements(rowEl, "hp:tc");

            boolean isHeaderRow = rowIndex == 0;
            boolean isOnlyOneBodyRow = rowCount == 2 && rowIndex == 1;
            boolean isFirstBodyRow = rowIndex == 1 && rowCount > 2;
            boolean isLastBodyRow = rowIndex == lastRowIndex && rowIndex > 1;
            boolean isMiddleBodyRow = rowIndex > 1 && rowIndex < lastRowIndex;

            int lastColIndex = cells.size() - 1;
            boolean isSingleCol = cells.size() == 1;

            for (int colIndex = 0; colIndex < cells.size(); colIndex++) {
                Element cellEl = cells.get(colIndex);
                boolean isFirstCol = colIndex == 0;
                boolean isLastCol = colIndex == lastColIndex;

                if (isHeaderRow) {
                    String headerBorderFillId;

                    if (isSingleCol) {
                        headerBorderFillId = pair.headerSingleColBorderFillId;
                    } else if (isFirstCol) {
                        headerBorderFillId = pair.headerFirstColBorderFillId;
                    } else if (isLastCol) {
                        headerBorderFillId = pair.headerLastColBorderFillId;
                    } else {
                        headerBorderFillId = pair.headerOtherColBorderFillId;
                    }

                    applyCellFullStyle(cellEl, pair.headerIds, pair.headerFontSize, headerBorderFillId);
                    continue;
                }

                if (isOnlyOneBodyRow) {
                    applyCellFullStyle(
                            cellEl,
                            pair.bodyIds,
                            pair.bodyFontSize,
                            isFirstCol
                                    ? pair.bodySingleRowFirstColBorderFillId
                                    : isLastCol
                                    ? pair.bodySingleRowLastColBorderFillId
                                    : pair.bodySingleRowOtherColBorderFillId
                    );
                    continue;
                }

                if (isFirstBodyRow) {
                    applyCellFullStyle(
                            cellEl,
                            pair.bodyIds,
                            pair.bodyFontSize,
                            isFirstCol
                                    ? pair.bodyFirstRowFirstColBorderFillId
                                    : isLastCol
                                    ? pair.bodyFirstRowLastColBorderFillId
                                    : pair.bodyFirstRowOtherColBorderFillId
                    );
                    continue;
                }

                if (isMiddleBodyRow) {
                    applyCellFullStyle(
                            cellEl,
                            pair.bodyIds,
                            pair.bodyFontSize,
                            isFirstCol
                                    ? pair.bodyMiddleRowFirstColBorderFillId
                                    : isLastCol
                                    ? pair.bodyMiddleRowLastColBorderFillId
                                    : pair.bodyMiddleRowOtherColBorderFillId
                    );
                    continue;
                }

                if (isLastBodyRow) {
                    applyCellFullStyle(
                            cellEl,
                            pair.bodyIds,
                            pair.bodyFontSize,
                            isFirstCol
                                    ? pair.bodyLastRowFirstColBorderFillId
                                    : isLastCol
                                    ? pair.bodyLastRowLastColBorderFillId
                                    : pair.bodyLastRowOtherColBorderFillId
                    );
                }
            }
        }
    }

    /**
     * oneline_box: 셀별 스타일 적용
     */
    private void applyStyleToOnelineBoxTable(
            Element tblEl,
            Map<String, RegisteredCellStyle> registeredCellMap
    ) {
        List<Element> rows = getElements(tblEl, "hp:tr");
        Map<Integer, Map<Integer, Boolean>> occupied = new HashMap<>();

        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Element rowEl = rows.get(rowIndex);
            List<Element> cells = getElements(rowEl, "hp:tc");

            int currentCol = 0;

            for (Element cellEl : cells) {
                Integer explicitRowAddr = readIntAttr(
                        cellEl,
                        "rowAddr", "rowaddr", "row", "rowIndex"
                );
                Integer explicitColAddr = readIntAttr(
                        cellEl,
                        "colAddr", "coladdr", "col", "colIndex"
                );

                int actualRow = explicitRowAddr != null ? explicitRowAddr : rowIndex;
                currentCol = explicitColAddr != null
                        ? explicitColAddr
                        : findNextAvailableCol(occupied, actualRow, currentCol);

                int rowSpan = readSpan(
                        cellEl,
                        "rowSpan", "rowspan", "rowSpanCount",
                        "vertSpan", "vSpan", "rowMerge"
                );
                int colSpan = readSpan(
                        cellEl,
                        "colSpan", "colspan", "colSpanCount",
                        "horzSpan", "hSpan", "colMerge"
                );

                RegisteredCellStyle registered = registeredCellMap.get(actualRow + ":" + currentCol);
                if (registered != null) {
                    applyCellFullStyle(
                            cellEl,
                            registered.ids,
                            registered.fontSize,
                            registered.borderFillId
                    );
                }

                markOccupied(occupied, actualRow, currentCol, rowSpan, colSpan);
                currentCol += colSpan;
            }
        }
    }

    /**
     * source paragraph text를 oneline_box 슬롯에 삽입
     * 예:
     * "4.1 사회경제지표자료" -> ["4.1", "사회경제지표자료"]
     * template 셀들 중 text가 있는 첫 셀과 마지막 셀에 주입
     */
    private void injectSourceTextIntoOnelineBox(Element tblEl, String sourceParagraphText) {
        if (tblEl == null) {
            return;
        }

        List<String> slotValues = splitSourceTextForOnelineBox(sourceParagraphText);

        List<Element> rows = getElements(tblEl, "hp:tr");
        if (rows.isEmpty()) {
            return;
        }

        List<Element> cells = getElements(rows.get(0), "hp:tc");
        if (cells.isEmpty()) {
            return;
        }

        List<Integer> nonEmptyTemplateCellIndexes = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            String templateCellText = extractCellText(cells.get(i));
            if (templateCellText != null && !templateCellText.isBlank()) {
                nonEmptyTemplateCellIndexes.add(i);
            }
        }

        if (nonEmptyTemplateCellIndexes.isEmpty()) {
            if (cells.size() == 1) {
                replaceCellText(cells.get(0), safe(slotValues, 0));
            } else {
                replaceCellText(cells.get(0), safe(slotValues, 0));
                replaceCellText(cells.get(cells.size() - 1), safe(slotValues, 1));
            }
            return;
        }

        if (slotValues.size() == 1) {
            replaceCellText(cells.get(nonEmptyTemplateCellIndexes.get(0)), slotValues.get(0));
            return;
        }

        replaceCellText(cells.get(nonEmptyTemplateCellIndexes.get(0)), slotValues.get(0));
        replaceCellText(cells.get(nonEmptyTemplateCellIndexes.get(nonEmptyTemplateCellIndexes.size() - 1)), slotValues.get(1));
    }

    private List<String> splitSourceTextForOnelineBox(String sourceParagraphText) {
        String normalized = normalizeCellText(sourceParagraphText);

        if (normalized.isBlank()) {
            return List.of("", "");
        }

        String[] parts = normalized.split("\\s+", 2);
        if (parts.length == 1) {
            return List.of(parts[0]);
        }

        return List.of(parts[0], parts[1]);
    }

    private String extractCellText(Element cellEl) {
        if (cellEl == null) {
            return "";
        }

        NodeList textNodes = cellEl.getElementsByTagName("hp:t");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < textNodes.getLength(); i++) {
            Node node = textNodes.item(i);
            if (node != null && node.getTextContent() != null) {
                sb.append(node.getTextContent());
            }
        }
        return normalizeCellText(sb.toString());
    }

    private void replaceCellText(Element cellEl, String newText) {
        if (cellEl == null) {
            return;
        }

        NodeList textNodes = cellEl.getElementsByTagName("hp:t");
        if (textNodes.getLength() == 0) {
            ensureCellHasBasicParagraph(cellEl, newText);
            return;
        }

        boolean first = true;
        for (int i = 0; i < textNodes.getLength(); i++) {
            Node node = textNodes.item(i);
            if (node == null) continue;

            node.setTextContent(first ? nvl(newText, "") : "");
            first = false;
        }
    }

    private void ensureCellHasBasicParagraph(Element cellEl, String text) {
        Document doc = cellEl.getOwnerDocument();

        Element subList = findDirectChild(cellEl, "hp:subList");
        if (subList == null) {
            subList = doc.createElement("hp:subList");
            cellEl.appendChild(subList);
        }

        Element p = doc.createElement("hp:p");
        Element run = doc.createElement("hp:run");
        Element t = doc.createElement("hp:t");
        t.setTextContent(nvl(text, ""));

        run.appendChild(t);
        p.appendChild(run);
        subList.appendChild(p);
    }

    private void applyCellFullStyle(
            Element cellEl,
            HwpxStyleRegistryService.RegisteredStyleIds ids,
            int fontSize,
            String overrideBorderFillId
    ) {
        if (cellEl == null || ids == null) {
            return;
        }

        String finalBorderFillId = (overrideBorderFillId != null && !overrideBorderFillId.isBlank())
                ? overrideBorderFillId
                : ids.getBorderFillId();

        if (finalBorderFillId != null && !finalBorderFillId.isBlank()) {
            cellEl.setAttribute("borderFillIDRef", finalBorderFillId);
        }

        List<Element> paraList = getElements(cellEl, "hp:p");
        for (Element paraEl : paraList) {
            if (ids.getParaPrId() != null && !ids.getParaPrId().isBlank()) {
                paraEl.setAttribute("paraPrIDRef", ids.getParaPrId());
            }

            List<Element> runList = getElements(paraEl, "hp:run");
            for (Element runEl : runList) {
                if (ids.getCharPrId() != null && !ids.getCharPrId().isBlank()) {
                    runEl.setAttribute("charPrIDRef", ids.getCharPrId());
                }
            }

            recalculateLineSegArray(paraEl, fontSize);
        }
    }

    private void recalculateLineSegArray(Element paraEl, int fontSize) {
        if (paraEl == null) {
            return;
        }

        int safeFontSize = fontSize > 0 ? fontSize : 10;

        int textHeight = safeFontSize * 100;
        int vertSize = textHeight;
        int baseline = (int) Math.round(textHeight * 0.85);
        int spacing = (int) Math.round(textHeight * 0.60);

        Element lineSegArray = findDirectChild(paraEl, "hp:linesegarray");
        if (lineSegArray == null) {
            lineSegArray = paraEl.getOwnerDocument().createElement("hp:linesegarray");
            paraEl.appendChild(lineSegArray);
        }

        List<Element> lineSegs = getDirectChildren(lineSegArray, "hp:lineseg");
        if (lineSegs.isEmpty()) {
            Element newLineSeg = paraEl.getOwnerDocument().createElement("hp:lineseg");
            newLineSeg.setAttribute("textpos", "0");
            newLineSeg.setAttribute("vertpos", "0");
            newLineSeg.setAttribute("vertsize", String.valueOf(vertSize));
            newLineSeg.setAttribute("textheight", String.valueOf(textHeight));
            newLineSeg.setAttribute("baseline", String.valueOf(baseline));
            newLineSeg.setAttribute("spacing", String.valueOf(spacing));
            newLineSeg.setAttribute("horzpos", "0");
            newLineSeg.setAttribute("horzsize", "23500");
            newLineSeg.setAttribute("flags", "393216");
            lineSegArray.appendChild(newLineSeg);
            return;
        }

        for (Element lineSeg : lineSegs) {
            lineSeg.setAttribute("textheight", String.valueOf(textHeight));
            lineSeg.setAttribute("vertsize", String.valueOf(vertSize));
            lineSeg.setAttribute("baseline", String.valueOf(baseline));
            lineSeg.setAttribute("spacing", String.valueOf(spacing));
        }
    }

    private boolean isOnelineBoxTemplate(TableNodeDto templateTable) {
        if (templateTable == null) {
            return false;
        }

        if (templateTable.getRowCount() == null || templateTable.getRowCount() != 1) {
            return false;
        }

        if (templateTable.getColCount() == null || templateTable.getColCount() < 2) {
            return false;
        }

        List<TableCellDto> cells = templateTable.getCells();
        if (cells == null || cells.isEmpty()) {
            return false;
        }

        int textCount = 0;
        boolean hasStyleDiff = false;

        String baseBg = null;
        Integer baseFontSize = null;
        Boolean baseBold = null;
        String baseFontColor = null;

        for (TableCellDto cell : cells) {
            String text = normalizeCellText(cell.getText());
            if (!text.isBlank()) {
                textCount++;
            }

            if (baseBg == null) baseBg = cell.getBackgroundColor();
            if (baseFontSize == null) baseFontSize = cell.getFontSize();
            if (baseBold == null) baseBold = cell.getBold();
            if (baseFontColor == null) baseFontColor = cell.getFontColor();

            if (!Objects.equals(baseBg, cell.getBackgroundColor())
                    || !Objects.equals(baseFontSize, cell.getFontSize())
                    || !Objects.equals(baseBold, cell.getBold())
                    || !Objects.equals(baseFontColor, cell.getFontColor())) {
                hasStyleDiff = true;
            }
        }

        return textCount > 0 && hasStyleDiff;
    }

    private String normalizeCellText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isParagraphElement(Element el) {
        if (el == null) {
            return false;
        }
        String nodeName = el.getNodeName();
        String localName = el.getLocalName();

        return "hp:p".equalsIgnoreCase(nodeName)
                || "p".equalsIgnoreCase(localName);
    }

    private TemplateOnelineBoxBlock extractTemplateOnelineBoxBlock(
            ZipFile templateZip,
            int targetTableIndex
    ) throws Exception {
        int[] currentTableIndexRef = new int[]{0};

        Enumeration<? extends ZipEntry> entries = templateZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            if (!entry.getName().startsWith("Contents/section")) continue;
            if (!entry.getName().endsWith(".xml")) continue;

            try (InputStream in = templateZip.getInputStream(entry)) {
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(in);

                NodeList tblList = doc.getElementsByTagName("hp:tbl");
                for (int i = 0; i < tblList.getLength(); i++) {
                    Node node = tblList.item(i);
                    if (!(node instanceof Element tblEl)) continue;

                    int currentIndex = currentTableIndexRef[0];
                    if (currentIndex == targetTableIndex) {
                        Element paragraphEl = findAncestorParagraph(tblEl);
                        if (paragraphEl == null) {
                            throw new IllegalArgumentException("template table parent paragraph not found");
                        }

                        return new TemplateOnelineBoxBlock(
                                toHeaderName(entry.getName()),
                                (Element) paragraphEl.cloneNode(true),
                                (Element) tblEl.cloneNode(true)
                        );
                    }
                    currentTableIndexRef[0] = currentIndex + 1;
                }
            }
        }

        return null;
    }

    private Element findAncestorParagraph(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof Element el) {
                String nodeName = el.getNodeName();
                String localName = el.getLocalName();

                if ("hp:p".equalsIgnoreCase(nodeName) || "p".equalsIgnoreCase(localName)) {
                    return el;
                }
            }
            current = current.getParentNode();
        }
        return null;
    }

    private Element findFirstDescendant(Element root, String tagName) {
        if (root == null) {
            return null;
        }

        NodeList list = root.getElementsByTagName(tagName);
        if (list.getLength() > 0 && list.item(0) instanceof Element el) {
            return el;
        }
        return null;
    }

    private Element findDirectChild(Element parent, String tagName) {
        if (parent == null) return null;

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            if (tagName.equalsIgnoreCase(nodeName) || tagName.equalsIgnoreCase(localName)) {
                return el;
            }
        }
        return null;
    }

    private List<Element> getDirectChildren(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        if (parent == null) return result;

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            if (tagName.equalsIgnoreCase(nodeName) || tagName.equalsIgnoreCase(localName)) {
                result.add(el);
            }
        }
        return result;
    }

    private String toHeaderName(String sectionName) {
        String headerName = sectionName.replace("Contents/section", "Contents/header");
        if (headerName.equals(sectionName)) {
            return "Contents/header0.xml";
        }
        return headerName;
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

    private int parseTargetTableIndex(String sourceTableId) {
        if (sourceTableId == null || sourceTableId.isBlank()) {
            return -1;
        }
        if (!sourceTableId.startsWith("t")) {
            return -1;
        }
        try {
            return Integer.parseInt(sourceTableId.substring(1));
        } catch (Exception e) {
            return -1;
        }
    }

    private int parseTargetParagraphIndex(String sourceParagraphId) {
        if (sourceParagraphId == null || sourceParagraphId.isBlank()) {
            return -1;
        }
        if (!sourceParagraphId.startsWith("p")) {
            return -1;
        }
        try {
            return Integer.parseInt(sourceParagraphId.substring(1));
        } catch (Exception e) {
            return -1;
        }
    }

    private Integer readIntAttr(Element el, String... names) {
        String value = readAnyAttr(el, names);
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int readSpan(Element el, String... attrNames) {
        String value = readAnyAttr(el, attrNames);

        if (value != null && !value.isBlank()) {
            try {
                return Math.max(1, Integer.parseInt(value.trim()));
            } catch (Exception ignored) {
            }
        }

        Integer nested = readNestedSpan(el, attrNames);
        return nested != null ? nested : 1;
    }

    private Integer readNestedSpan(Element el, String... attrNames) {
        if (el == null) {
            return null;
        }

        NodeList children = el.getElementsByTagName("*");
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element child)) {
                continue;
            }

            String value = readAnyAttr(child, attrNames);
            if (value != null && !value.isBlank()) {
                try {
                    return Math.max(1, Integer.parseInt(value.trim()));
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private String readAnyAttr(Element el, String... names) {
        if (el == null || names == null) {
            return null;
        }

        if (el.getAttributes() == null) {
            return null;
        }

        for (String name : names) {
            Node node = el.getAttributes().getNamedItem(name);
            if (node != null) {
                return node.getNodeValue();
            }

            for (int i = 0; i < el.getAttributes().getLength(); i++) {
                Node attr = el.getAttributes().item(i);
                if (attr == null) continue;

                String nodeName = attr.getNodeName();
                String localName = attr.getLocalName();

                if (name.equalsIgnoreCase(nodeName) || name.equalsIgnoreCase(localName)) {
                    return attr.getNodeValue();
                }
            }
        }

        return null;
    }

    private int findNextAvailableCol(
            Map<Integer, Map<Integer, Boolean>> occupied,
            int rowIndex,
            int startCol
    ) {
        int col = startCol;

        while (occupied
                .getOrDefault(rowIndex, Map.of())
                .getOrDefault(col, false)) {
            col++;
        }

        return col;
    }

    private void markOccupied(
            Map<Integer, Map<Integer, Boolean>> occupied,
            int rowIndex,
            int colIndex,
            int rowSpan,
            int colSpan
    ) {
        for (int r = rowIndex; r < rowIndex + rowSpan; r++) {
            Map<Integer, Boolean> rowMap = occupied.computeIfAbsent(r, k -> new HashMap<>());
            for (int c = colIndex; c < colIndex + colSpan; c++) {
                rowMap.put(c, true);
            }
        }
    }

    private String safe(List<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }
        return values.get(index);
    }

    private String nvl(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    private byte[] documentToBytes(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(baos));
        return baos.toByteArray();
    }

    private record TemplateOnelineBoxBlock(
            String headerName,
            Element paragraphElement,
            Element tableElement
    ) {
    }

    private static class RegisteredPair {
        private HwpxStyleRegistryService.RegisteredStyleIds headerIds;
        private HwpxStyleRegistryService.RegisteredStyleIds bodyIds;

        private String headerFirstColBorderFillId;
        private String headerOtherColBorderFillId;
        private String headerLastColBorderFillId;
        private String headerSingleColBorderFillId;

        private String bodyFirstRowFirstColBorderFillId;
        private String bodyMiddleRowFirstColBorderFillId;
        private String bodyLastRowFirstColBorderFillId;
        private String bodySingleRowFirstColBorderFillId;

        private String bodyFirstRowOtherColBorderFillId;
        private String bodyMiddleRowOtherColBorderFillId;
        private String bodyLastRowOtherColBorderFillId;
        private String bodySingleRowOtherColBorderFillId;

        private String bodyFirstRowLastColBorderFillId;
        private String bodyMiddleRowLastColBorderFillId;
        private String bodyLastRowLastColBorderFillId;
        private String bodySingleRowLastColBorderFillId;

        private int headerFontSize;
        private int bodyFontSize;
    }

    private static class RegisteredCellStyle {
        private HwpxStyleRegistryService.RegisteredStyleIds ids;
        private String borderFillId;
        private int fontSize;
    }
}