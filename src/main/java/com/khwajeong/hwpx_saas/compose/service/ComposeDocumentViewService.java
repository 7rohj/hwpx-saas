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
     * 블록 간 여백.
     * 너무 크면 같은 페이지에 들어갈 요소도 다음 페이지로 넘어가므로
     * 기존보다 줄여서 좀 더 촘촘하게 배치합니다.
     */
    private static final double PREVIEW_BLOCK_GAP = 12.0;

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

        // 1. 문단 추출
        List<ParagraphNodeDto> paragraphs =
                composeService.extractFromHwpx(hwpxFile);

        // 2. 표 추출
        List<TableNodeDto> tables =
                hwpxTableExtractor.extract(hwpxFile);

        // 3. 공통 레이아웃 계산
        List<PageLayoutBlockDto> layout =
                composeLayoutService.buildLayout(paragraphs, tables);

        // 4. paragraph 좌표 반영
        composeLayoutService.applyLayoutToParagraphs(paragraphs, layout);

        // 5. table 좌표 반영
        composeLayoutService.applyLayoutToTables(tables, layout);

        /**
         * 핵심 보완:
         * 문단/표 순서를 원본 문서 등장 순서대로 다시 읽고,
         * preview 전용 page / y 를 그 순서대로 재배치합니다.
         */
        reflowByDocumentOrder(hwpxFile, paragraphs, tables);

        // 6. 결과 반환
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

                /**
                 * 중요:
                 * 프론트 preview에서는 표 셀 높이를 1/2 보정해서 렌더링합니다.
                 * 그런데 백엔드가 page break 계산에서는 원래 큰 높이를 쓰면,
                 * 실제로는 첫 페이지에 들어갈 표도 미리 다음 페이지로 밀어버립니다.
                 *
                 * 그래서 page break 계산용 blockHeight는
                 * "preview에서 실제 보일 높이"와 유사하게 추정해서 사용합니다.
                 */
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

        // preview에서는 각 셀 세로 길이를 기존 계산값의 1/2로 강제 보정
        return fullHeight * 0.5;
    }

    /**
     * 원본 HWPX 문서에서 "문단 / 표" 등장 순서를 그대로 추출
     *
     * 결과 예:
     *   p, p, p, t, t, p, p, p, t
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

            if (containsTableDescendant(element)) {
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

    private boolean containsTableDescendant(Element p) {
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

    // =========================
    // 내부 DTO (응답용)
    // =========================
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