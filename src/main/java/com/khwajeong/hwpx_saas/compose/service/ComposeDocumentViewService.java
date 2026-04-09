package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.dto.PageLayoutBlockDto;
import com.khwajeong.hwpx_saas.compose.dto.ParagraphNodeDto;
import com.khwajeong.hwpx_saas.compose.dto.TableNodeDto;
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
    private static final double PREVIEW_BLOCK_GAP = 22.0;

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
         * 기존에는 paragraph / table 이 서로 다른 좌표 기준을 가진 상태에서
         * 프론트가 y 값만 보고 합쳐 정렬하다 보니,
         * 실제 문서 순서와 preview 순서가 뒤섞이는 문제가 있었습니다.
         *
         * 그래서 여기서 원본 HWPX XML의 실제 등장 순서를 다시 읽고,
         * preview용 page/y 를 그 순서대로 재배치합니다.
         *
         * 결과:
         * - 제1장 heading1
         * - 1. 개요 heading2
         * - 본문내용입니다 body
         * - 표1
         * - 표2
         * - 2. 내용설명 heading2
         * 같은 원문 순서가 preview에서도 유지됩니다.
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
                double blockHeight = p.getHeight() != null ? p.getHeight() : 34.0;

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
                double blockHeight = t.getHeight() != null ? t.getHeight() : 120.0;

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

    /**
     * 원본 HWPX 문서에서 "문단 / 표" 등장 순서를 그대로 추출
     *
     * 결과 예:
     *   p, p, p, t, t, p, p, p, t
     *
     * 여기서는 문단 ID / 표 ID 자체를 XML에서 읽는 게 아니라
     * extractor와 동일한 순서 규칙으로 카운트만 맞춥니다.
     * 실제 preview 좌표 재배치는 paragraphs/tables 정렬된 리스트를
     * 순서대로 소모하는 방식으로 처리합니다.
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
            // 표 내부 문단 제외
            if (isInsideTable(element)) {
                return;
            }

            // 표를 감싸는 wrapper 문단 제외
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