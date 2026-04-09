package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.PageLayoutBlockDto;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
import com.khwajeong.hwpx_saas.compose.model.ExtractedTableStyle;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class ComposeDocumentViewService {

    private static final double PREVIEW_PAGE_HEIGHT_LIMIT = 680.0;
    private static final double PREVIEW_START_Y = 40.0;

    /**
     * 페이지 간격이 너무 보수적이면 실제로 들어갈 수 있는 표도 다음 페이지로 넘어간다.
     * 그래서 기본 block gap 을 더 줄여서 첫 페이지 수용력을 높인다.
     */
    private static final double PREVIEW_BLOCK_GAP = 8.0;

    private final ComposeService composeService;
    private final HwpxTableExtractor hwpxTableExtractor;
    private final ComposeLayoutService composeLayoutService;

    public ComposeDocumentViewService(
            ComposeService composeService,
            HwpxTableExtractor hwpxTableExtractor,
            ComposeLayoutService composeLayoutService
    ) {
        this.composeService = composeService;
        this.hwpxTableExtractor = hwpxTableExtractor;
        this.composeLayoutService = composeLayoutService;
    }

    public DocumentViewResult build(File hwpxFile) {

        if (hwpxFile == null || !hwpxFile.exists()) {
            throw new IllegalArgumentException("hwpx file not found");
        }

        List<ParagraphNodeDto> paragraphs =
                composeService.extractFromHwpx(hwpxFile);

        List<TableNodeDto> tables =
                hwpxTableExtractor.extract(hwpxFile);

        List<PageLayoutBlockDto> layout =
                composeLayoutService.buildLayout(paragraphs, tables);

        composeLayoutService.applyLayoutToParagraphs(paragraphs, layout);
        composeLayoutService.applyLayoutToTables(tables, layout);

        /**
         * 문단 / 표 순서를 원본 HWPX XML 기준으로 다시 잡아 preview page/y 에 반영
         */
        reflowByDocumentOrder(hwpxFile, paragraphs, tables);

        DocumentViewResult result = new DocumentViewResult();
        result.setParagraphs(paragraphs);
        result.setTables(tables);

        return result;
    }

    private void reflowByDocumentOrder(
            File hwpxFile,
            List<ParagraphNodeDto> paragraphs,
            List<TableNodeDto> tables
    ) {
        List<String> sequence = extractDocumentSequence(hwpxFile);

        if (sequence.isEmpty()) {
            return;
        }

        paragraphs.sort(Comparator.comparingInt(p -> safeIndexFromId(p.getParagraphId(), "p")));
        tables.sort(Comparator.comparingInt(t -> safeIndexFromId(t.getTableId(), "t")));

        int paragraphCursor = 0;
        int tableCursor = 0;

        int page = 1;
        double cursorY = PREVIEW_START_Y;

        for (String key : sequence) {
            if (key == null || key.isBlank()) {
                continue;
            }

            if (key.startsWith("p")) {
                if (paragraphCursor >= paragraphs.size()) {
                  continue;
                }

                ParagraphNodeDto p = paragraphs.get(paragraphCursor++);
                double blockHeight = estimateParagraphPreviewHeight(p);

                if (cursorY + blockHeight > PREVIEW_PAGE_HEIGHT_LIMIT) {
                    page++;
                    cursorY = PREVIEW_START_Y;
                }

                p.setPage(page);
                p.setY(cursorY);

                cursorY += blockHeight + PREVIEW_BLOCK_GAP;
                continue;
            }

            if (key.startsWith("t")) {
                if (tableCursor >= tables.size()) {
                    continue;
                }

                TableNodeDto t = tables.get(tableCursor++);
                double blockHeight = estimateTablePreviewHeight(t);

                if (cursorY + blockHeight > PREVIEW_PAGE_HEIGHT_LIMIT) {
                    page++;
                    cursorY = PREVIEW_START_Y;
                }

                t.setPage(page);
                t.setY(cursorY);

                cursorY += blockHeight + PREVIEW_BLOCK_GAP;
            }
        }

        /**
         * sequence 추출에서 혹시 누락된 잔여 paragraph / table 이 있더라도
         * 마지막에 이어서 배치해서 preview가 비지 않도록 보완
         */
        while (paragraphCursor < paragraphs.size()) {
            ParagraphNodeDto p = paragraphs.get(paragraphCursor++);
            double blockHeight = estimateParagraphPreviewHeight(p);

            if (cursorY + blockHeight > PREVIEW_PAGE_HEIGHT_LIMIT) {
                page++;
                cursorY = PREVIEW_START_Y;
            }

            p.setPage(page);
            p.setY(cursorY);
            cursorY += blockHeight + PREVIEW_BLOCK_GAP;
        }

        while (tableCursor < tables.size()) {
            TableNodeDto t = tables.get(tableCursor++);
            double blockHeight = estimateTablePreviewHeight(t);

            if (cursorY + blockHeight > PREVIEW_PAGE_HEIGHT_LIMIT) {
                page++;
                cursorY = PREVIEW_START_Y;
            }

            t.setPage(page);
            t.setY(cursorY);
            cursorY += blockHeight + PREVIEW_BLOCK_GAP;
        }
    }

    private double estimateParagraphPreviewHeight(ParagraphNodeDto p) {
        String text = p.getText() == null ? "" : p.getText();
        int lineCount = Math.max(1, text.split("\\R", -1).length);
        int fontSize = (p.getFontSize() == null || p.getFontSize() <= 0) ? 12 : p.getFontSize();

        double estimated = lineCount * (fontSize * 1.6 + 6);
        return Math.max(34.0, Math.min(64.0, estimated));
    }

    private double estimateTablePreviewHeight(TableNodeDto t) {
        int rowCount = Math.max(1, t.getRowCount() != null ? t.getRowCount() : 1);

        int headerFontSize = 11;
        int bodyFontSize = 11;

        ExtractedTableStyle style = t.getExtractedStyle();
        if (style != null) {
            if (style.getHeaderTextStyle() != null
                    && style.getHeaderTextStyle().getFontSize() != null
                    && style.getHeaderTextStyle().getFontSize() > 0) {
                headerFontSize = style.getHeaderTextStyle().getFontSize();
            }
            if (style.getBodyTextStyle() != null
                    && style.getBodyTextStyle().getFontSize() != null
                    && style.getBodyTextStyle().getFontSize() > 0) {
                bodyFontSize = style.getBodyTextStyle().getFontSize();
            }
        } else if (t.getFontSize() != null && t.getFontSize() > 0) {
            headerFontSize = t.getFontSize();
            bodyFontSize = t.getFontSize();
        }

        double headerHeight = Math.max(30.0, (headerFontSize + 10) * 1.5);
        double bodyHeight = Math.max(28.0, (bodyFontSize + 10) * 1.45);

        double fullHeight = headerHeight + Math.max(0, rowCount - 1) * bodyHeight;

        /**
         * preview에서는 표 셀 세로 길이를 1/2로 보정해서 렌더하므로
         * page break 판단도 같은 기준을 따라가야 한다.
         */
        return fullHeight * 0.5;
    }

    /**
     * 원본 HWPX 문서에서 문단 / 표 등장 순서를 그대로 추출
     */
    private List<String> extractDocumentSequence(File hwpxFile) {
        List<String> sequence = new ArrayList<>();

        try (ZipFile zipFile = new ZipFile(hwpxFile)) {
            List<ZipEntry> sectionEntries = findSectionEntries(zipFile);

            for (ZipEntry entry : sectionEntries) {
                byte[] sectionBytes;
                try (InputStream in = zipFile.getInputStream(entry)) {
                    sectionBytes = in.readAllBytes();
                }

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);

                Document doc = factory.newDocumentBuilder()
                        .parse(new InputSource(new ByteArrayInputStream(sectionBytes)));

                Element root = doc.getDocumentElement();
                if (root == null) {
                    continue;
                }

                walkDocumentOrder(root, sequence);
            }
        } catch (Exception e) {
            throw new RuntimeException("failed to extract document sequence", e);
        }

        return sequence;
    }

    private void walkDocumentOrder(Element element, List<String> sequence) {
        if (element == null) {
            return;
        }

        String nodeName = element.getNodeName();
        String localName = element.getLocalName();

        if (isTableElement(nodeName, localName)) {
            sequence.add("t");
            return;
        }

        if (isParagraphElement(nodeName, localName)) {
            if (isInsideTable(element)) {
                return;
            }

            /**
             * 중요:
             * 표를 감싸는 wrapper paragraph는 sequence에 "문단"으로 넣으면 안 되지만,
             * 그 안에 실제 table child가 있으면 child traversal은 계속해야 한다.
             *
             * 이전에는 여기서 바로 return 해버리면 안쪽 table이 sequence에서 빠져서,
             * 마지막 표가 재배치 대상에서 누락되고 원래 page 값(예: 2page)을 유지할 수 있었다.
             */
            if (containsDirectOrNestedTable(element)) {
                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element childEl) {
                        walkDocumentOrder(childEl, sequence);
                    }
                }
                return;
            }

            String text = extractParagraphText(element);
            if (text != null && !text.trim().isEmpty()) {
                sequence.add("p");
            }
            return;
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childEl) {
                walkDocumentOrder(childEl, sequence);
            }
        }
    }

    private boolean isParagraphElement(String nodeName, String localName) {
        return "hp:p".equalsIgnoreCase(nodeName) || "p".equalsIgnoreCase(localName);
    }

    private boolean isTableElement(String nodeName, String localName) {
        return "hp:tbl".equalsIgnoreCase(nodeName) || "tbl".equalsIgnoreCase(localName);
    }

    private boolean isInsideTable(Node node) {
        Node parent = node.getParentNode();

        while (parent != null) {
            if (parent instanceof Element el) {
                String nodeName = el.getNodeName();
                String localName = el.getLocalName();

                if (isTableElement(nodeName, localName)) {
                    return true;
                }
            }
            parent = parent.getParentNode();
        }

        return false;
    }

    private boolean containsDirectOrNestedTable(Element p) {
        if (p == null) {
            return false;
        }

        NodeList tblList = p.getElementsByTagName("hp:tbl");
        if (tblList != null && tblList.getLength() > 0) {
            return true;
        }

        NodeList children = p.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) {
                continue;
            }

            String nodeName = el.getNodeName();
            String localName = el.getLocalName();

            if (isTableElement(nodeName, localName)) {
                return true;
            }
        }

        return false;
    }

    private String extractParagraphText(Element p) {
        NodeList tNodes = p.getElementsByTagName("hp:t");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tNodes.getLength(); i++) {
            Node n = tNodes.item(i);
            if (n != null) {
                String text = n.getTextContent();
                if (text != null) {
                    sb.append(text);
                }
            }
        }

        return sb.toString();
    }

    private List<ZipEntry> findSectionEntries(ZipFile zipFile) {
        List<ZipEntry> result = new ArrayList<>();

        zipFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().startsWith("Contents/section"))
                .filter(entry -> entry.getName().endsWith(".xml"))
                .sorted(Comparator.comparing(ZipEntry::getName))
                .forEach(result::add);

        return result;
    }

    private int safeIndexFromId(String id, String prefix) {
        if (id == null || id.isBlank()) {
            return Integer.MAX_VALUE;
        }

        String v = id.trim();
        if (v.startsWith(prefix)) {
            v = v.substring(prefix.length());
        }

        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    public static class DocumentViewResult {
        private List<ParagraphNodeDto> paragraphs;
        private List<TableNodeDto> tables;

        public List<ParagraphNodeDto> getParagraphs() {
            return paragraphs;
        }

        public void setParagraphs(List<ParagraphNodeDto> paragraphs) {
            this.paragraphs = paragraphs;
        }

        public List<TableNodeDto> getTables() {
            return tables;
        }

        public void setTables(List<TableNodeDto> tables) {
            this.tables = tables;
        }
    }
}