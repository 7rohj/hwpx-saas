package io.github.mumberrymountain.render.style;

import kr.dogfoot.hwpxlib.object.content.header_xml.HeaderXMLFile;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.BorderFill;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.CharPr;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.ParaPr;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.HorizontalAlign2; //추가
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.VerticalAlign1; // 추가
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.ValueUnit2;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.LineSpacingType;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.parapr.ParaMargin; // 타입명이 다르면 IDE로 margin() 리턴타입 확인
import io.github.mumberrymountain.model.Text;
import io.github.mumberrymountain.model.table.Align;
import io.github.mumberrymountain.model.table.Cell;
import io.github.mumberrymountain.model.table.Table;
import io.github.mumberrymountain.render.RendererUtil;

import java.util.HashMap;
import java.util.Map;

public class StyleRenderer {
    private final HeaderXMLFile headerXMLFile;
    private final Map<String, CharPr> charPrs = new HashMap<String, CharPr>();
    private final Map<String, ParaPr> paraPrs = new HashMap<String, ParaPr>();
    private final Map<String, BorderFill> borderFillIds = new HashMap<String, BorderFill>();

    public StyleRenderer (HeaderXMLFile headerXMLFile){
        this.headerXMLFile = headerXMLFile;
    }

    public String renderCharStyleAndReturnCharPrId(Text text) {
        String key = RendererUtil.createCharStyleKey(text);

        if (charPrs.containsKey(key)) return charPrs.get(key).id();

        String fontId = new FontRenderer(headerXMLFile, text.getFontFamily()).render();
        CharPr charPr = new CharPrRenderer(headerXMLFile, fontId, text).render();

        headerXMLFile.refList().charProperties().add(charPr);
        charPrs.put(key, charPr);

        return charPr.id();
    }

    public String renderBorderStyle(Cell cell) {
        String key = RendererUtil.createBorderStyleKey(cell);
        if (borderFillIds.containsKey(key)) return borderFillIds.get(key).id();

        BorderFill borderFill = new BorderRenderer(headerXMLFile, cell).render();
        borderFillIds.put(key, borderFill);

        return borderFill.id();
    }

    // 260307 수정
    public String renderBorderStyle(Cell cell, Table table, int rowIdx, int colIdx, String bgColor) {
        if (cell == null || table == null) return renderBorderStyle(cell);

        Map<String, Object> styleMap = safeMap(table.getConfig("style"));
        Object borderObj = null;

        if (styleMap != null) borderObj = styleMap.get("border");
        if (!(borderObj instanceof Map)) borderObj = table.getConfig("border");

        if (!(borderObj instanceof Map)) {
            return renderBorderStyle(cell);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> border = (Map<String, Object>) borderObj;

        Map<String, Object> outer = safeMap(border.get("outer"));
        Map<String, Object> inner = safeMap(border.get("inner"));

        if (outer == null && inner == null) return renderBorderStyle(cell);
        if (outer == null) outer = inner;
        if (inner == null) inner = outer;

        int cs = Math.max(reflectInt(cell, "getColSpan", 1), 1);
        int rs = Math.max(reflectInt(cell, "getRowSpan", 1), 1);
        int endCol = colIdx + cs - 1;
        int endRow = rowIdx + rs - 1;

        boolean touchesTop = (rowIdx == 0);
        boolean touchesLeft = (colIdx == 0);
        boolean touchesBottom = (endRow == table.getRowCount() - 1);
        boolean touchesRight = (endCol == table.getColCount() - 1);

        Map<String, Object> leftSpec   = copySpec(touchesLeft   ? outer : inner);
        Map<String, Object> rightSpec  = copySpec(touchesRight  ? outer : inner);
        Map<String, Object> topSpec    = copySpec(touchesTop    ? outer : inner);
        Map<String, Object> bottomSpec = copySpec(touchesBottom ? outer : inner);

        // -------------------------
        // header / lastRow override
        // -------------------------
        Map<String, Object> headerSpec = safeMap(border.get("header"));
        Map<String, Object> lastRowSpec = safeMap(border.get("lastRow"));

        int headerRow = resolveHeaderRow(styleMap);

        // header row override
        if (rowIdx == headerRow) {
            Map<String, Object> hBottom = safeMap(headerSpec == null ? null : headerSpec.get("bottom"));
            Map<String, Object> hTop = safeMap(headerSpec == null ? null : headerSpec.get("top"));
            Map<String, Object> hLeft = safeMap(headerSpec == null ? null : headerSpec.get("left"));
            Map<String, Object> hRight = safeMap(headerSpec == null ? null : headerSpec.get("right"));
            Map<String, Object> hVerticalInner = safeMap(headerSpec == null ? null : headerSpec.get("verticalInner"));
            Map<String, Object> hHorizontalInner = safeMap(headerSpec == null ? null : headerSpec.get("horizontalInner"));

            if (hTop != null && touchesTop) topSpec = copySpec(hTop);
            if (hBottom != null) bottomSpec = copySpec(hBottom);
            if (hLeft != null && touchesLeft) leftSpec = copySpec(hLeft);
            if (hRight != null && touchesRight) rightSpec = copySpec(hRight);

            // 헤더행 내부 세로선: 각 셀의 right 기준으로 적용 (마지막 열 제외)
            if (hVerticalInner != null && !touchesRight) {
                rightSpec = copySpec(hVerticalInner);
            }

            // 헤더행 내부 가로선: 보통 헤더 내부 하단/중간선에 대응
            if (hHorizontalInner != null && !touchesBottom) {
                bottomSpec = copySpec(hHorizontalInner);
            }
        }

        // last row override
        if (endRow == table.getRowCount() - 1) {
            Map<String, Object> lrBottom = safeMap(lastRowSpec == null ? null : lastRowSpec.get("bottom"));
            Map<String, Object> lrTop = safeMap(lastRowSpec == null ? null : lastRowSpec.get("top"));
            Map<String, Object> lrLeft = safeMap(lastRowSpec == null ? null : lastRowSpec.get("left"));
            Map<String, Object> lrRight = safeMap(lastRowSpec == null ? null : lastRowSpec.get("right"));

            if (lrBottom != null) bottomSpec = copySpec(lrBottom);
            if (lrTop != null) topSpec = copySpec(lrTop);
            if (lrLeft != null && touchesLeft) leftSpec = copySpec(lrLeft);
            if (lrRight != null && touchesRight) rightSpec = copySpec(lrRight);
        }

        // -------------------------
        // borderVariation override (최종 우선)
        // -------------------------
        if (styleMap != null) {
            Object bvObj = styleMap.get("borderVariation");
            if (bvObj instanceof Iterable<?>) {
                for (Object item : (Iterable<?>) bvObj) {
                    Map<String, Object> rule = safeMap(item);
                    if (rule == null) continue;
                    if (!matchesVariationRule(rule, rowIdx, colIdx, endRow, endCol, table, headerRow)) continue;

                    Map<String, Object> vTop = safeMap(rule.get("top"));
                    Map<String, Object> vBottom = safeMap(rule.get("bottom"));
                    Map<String, Object> vLeft = safeMap(rule.get("left"));
                    Map<String, Object> vRight = safeMap(rule.get("right"));
                    Map<String, Object> vVerticalInner = safeMap(rule.get("verticalInner"));
                    Map<String, Object> vHorizontalInner = safeMap(rule.get("horizontalInner"));

                    if (vTop != null) topSpec = copySpec(vTop);
                    if (vBottom != null) bottomSpec = copySpec(vBottom);
                    if (vLeft != null) leftSpec = copySpec(vLeft);
                    if (vRight != null) rightSpec = copySpec(vRight);

                    if (vVerticalInner != null && !touchesRight) {
                        rightSpec = copySpec(vVerticalInner);
                    }
                    if (vHorizontalInner != null && !touchesBottom) {
                        bottomSpec = copySpec(vHorizontalInner);
                    }
                }
            }
        }

        // -------------------------
        // shared-edge 우선권 정리
        // - 가로 공유선: 위 셀 bottom 우선, 아래 셀 top 숨김
        // - 세로 공유선: 왼쪽 셀 right 우선, 오른쪽 셀 left 숨김
        // -------------------------

        boolean isFirstRow = (rowIdx == 0);
        boolean isFirstCol = (colIdx == 0);
        boolean isLastRow = (endRow == table.getRowCount() - 1);
        boolean isLastCol = (endCol == table.getColCount() - 1);

        // 내부 top/left는 기본적으로 숨겨서 중복선 방지
        if (!isFirstRow) {
            topSpec = hiddenSpec();
        }
        if (!isFirstCol) {
            leftSpec = hiddenSpec();
        }

        // 헤더 아래 굵은 선이 있으면, 그 바로 아래 행의 top은 반드시 숨김
        Map<String, Object> headerSpec2 = safeMap(border.get("header"));
        Map<String, Object> hBottom2 = safeMap(headerSpec2 == null ? null : headerSpec2.get("bottom"));
        if (hBottom2 != null && rowIdx == headerRow + 1) {
            topSpec = hiddenSpec();
        }

        // 헤더 내부 세로선이 있으면, 헤더행에서 오른쪽 셀의 left는 숨김
        Map<String, Object> hVerticalInner2 = safeMap(headerSpec2 == null ? null : headerSpec2.get("verticalInner"));
        if (hVerticalInner2 != null && rowIdx == headerRow && !isFirstCol) {
            leftSpec = hiddenSpec();
        }

        // 마지막 행 아래선은 마지막 행의 bottom이 대표
        Map<String, Object> lastRowSpec2 = safeMap(border.get("lastRow"));
        Map<String, Object> lrBottom2 = safeMap(lastRowSpec2 == null ? null : lastRowSpec2.get("bottom"));
        if (lrBottom2 != null && !isLastRow && rowIdx == table.getRowCount() - 2) {
            // 마지막 행 바로 윗행은 자기 bottom을 inner로 두고,
            // 마지막 행이 최종 bottom 대표를 가지게 둠
        }
        // -------------------------

        String key =
                RendererUtil.createBorderStyleKey(cell) +
                "|L=" + borderSpecKey(leftSpec) +
                "|R=" + borderSpecKey(rightSpec) +
                "|T=" + borderSpecKey(topSpec) +
                "|B=" + borderSpecKey(bottomSpec) +
                "|BG=" + String.valueOf(normalizeHex(bgColor));

        if (borderFillIds.containsKey(key)) return borderFillIds.get(key).id();

        BorderFill borderFill = new BorderRenderer(
                headerXMLFile,
                cell,
                leftSpec, rightSpec, topSpec, bottomSpec
        ).render();

        applyBackgroundColor(borderFill, bgColor);

        borderFillIds.put(key, borderFill);
        return borderFill.id();
    }

    public String renderParaStyleAndReturnParaPrId(Align align) {
        if (align == null) align = Align.Left;
        String key = RendererUtil.createParaStyleKey(align);
        if (paraPrs.containsKey(key)) return paraPrs.get(key).id();

        ParaPr paraPr = new ParaPrRenderer(headerXMLFile, align).render();

        headerXMLFile.refList().paraProperties().add(paraPr);
        paraPrs.put(key, paraPr);

        return paraPr.id();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object o) {
        if (o instanceof Map) return (Map<String, Object>) o;
        return null;
    }

    private String borderSpecKey(Map<String, Object> spec) {
        if (spec == null) return "null";
        Object st = spec.get("style");
        Object w = spec.get("width");
        Object c = spec.get("color");
        return String.valueOf(st) + "|" + String.valueOf(w) + "|" + String.valueOf(c);
    }

    private int reflectInt(Object target, String method, int fallback) {
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(method);
            Object r = m.invoke(target);
            if (r instanceof Number) return ((Number) r).intValue();
        } catch (Exception ignored) {}
        return fallback;
    }

    private void applyBackgroundColor(BorderFill borderFill, String bgColor) {
        String rgb = normalizeHex(bgColor);
        if (rgb == null) return;

        if (borderFill.fillBrush() == null) borderFill.createFillBrush();
        if (borderFill.fillBrush().winBrush() == null) borderFill.fillBrush().createWinBrush();

        // RGB -> BGR
        String bgr6 = rgbToBgr(rgb);

        String bgr8 = "00" + bgr6;

        borderFill.fillBrush().winBrush().faceColor(bgr8);
        borderFill.fillBrush().winBrush().hatchColor(bgr8);
    }

    // "#eeeeee" / "eeeeee" / null -> "EEEEEE" (6자리) or null
    private String normalizeHex(String hex) {
        if (hex == null) return null;
        String s = hex.trim();
        if (s.isEmpty()) return null;

        if (s.startsWith("#")) s = s.substring(1);
        s = s.trim();

        if (s.length() == 3) {
            char r = s.charAt(0), g = s.charAt(1), b = s.charAt(2);
            s = "" + r + r + g + g + b + b;
        }

        if (s.length() != 6) return null;

        // hex 체크
        for (int i = 0; i < 6; i++) {
            char ch = s.charAt(i);
            boolean ok = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (!ok) return null;
        }

        return s.toUpperCase();
    }

    private String rgbToBgr(String rgb6) {
        if (rgb6 == null || rgb6.length() != 6) return rgb6;
        String rr = rgb6.substring(0, 2);
        String gg = rgb6.substring(2, 4);
        String bb = rgb6.substring(4, 6);
        return (bb + gg + rr).toUpperCase(); // BGR
    }

    // 260227
    // (2개 인자) 호환 유지
    public String renderParaStyleFromBaseAndReturnParaPrId(String baseParaPrId, Align align) {
        return renderParaStyleFromBaseAndReturnParaPrId(baseParaPrId, align, null, null, null);
    }

    // (3개 인자) 호환 유지: 기존 호출부(TextObjectReplacementRenderer)가 이걸 부름
    public String renderParaStyleFromBaseAndReturnParaPrId(String baseParaPrId, Align align, Integer lineSpacingPercent) {
        return renderParaStyleFromBaseAndReturnParaPrId(baseParaPrId, align, lineSpacingPercent, null, null);
    }

    // (5개 인자) 최종: align + 줄간격 + 앞/뒤 여백(pt)
    public String renderParaStyleFromBaseAndReturnParaPrId(
            String baseParaPrId,
            Align align,
            Integer lineSpacingPercent,
            Integer spaceBeforePt,
            Integer spaceAfterPt
    ) {
        if (align == null) align = Align.Left;

        if (baseParaPrId == null || baseParaPrId.isBlank()) {
            return renderParaStyleAndReturnParaPrId(align);
        }

        String key = "BASE=" + baseParaPrId
            + "|ALIGN=" + align.name()
            + "|LS=" + (lineSpacingPercent == null ? "null" : lineSpacingPercent)
            + "|SB=" + (spaceBeforePt == null ? "null" : spaceBeforePt)
            + "|SA=" + (spaceAfterPt == null ? "null" : spaceAfterPt);

        if (paraPrs.containsKey(key)) return paraPrs.get(key).id();

        ParaPr base = null;
        for (ParaPr p : headerXMLFile.refList().paraProperties().items()) {
            if (baseParaPrId.equals(p.id())) {
                base = p;
                break;
            }
        }
        if (base == null) {
            return renderParaStyleAndReturnParaPrId(align);
        }

        ParaPr cloned = base.clone();

        String newId = Integer.toString(headerXMLFile.refList().paraProperties().count());
        cloned.id(newId);

        HorizontalAlign2 ha = HorizontalAlign2.LEFT;
        switch (align) {
            case Center: ha = HorizontalAlign2.CENTER; break;
            case Right:  ha = HorizontalAlign2.RIGHT;  break;
            case Left:
            default:     ha = HorizontalAlign2.LEFT;   break;
        }
        cloned.createAlign();
        cloned.align().horizontal(ha);
        cloned.align().vertical(VerticalAlign1.BASELINE);

        if (lineSpacingPercent != null) {
            cloned.createLineSpacing();
            cloned.lineSpacing().type(LineSpacingType.PERCENT);
            cloned.lineSpacing().value(lineSpacingPercent);
        }

        Integer prevHwp = (spaceBeforePt == null) ? null : spaceBeforePt * 200; // * 100?
        Integer nextHwp = (spaceAfterPt == null) ? null : spaceAfterPt * 200;   // * 100?
        setParaMarginPrevNext(cloned, prevHwp, nextHwp);

        headerXMLFile.refList().paraProperties().add(cloned);
        paraPrs.put(key, cloned);

        return cloned.id();
    }

    private void setParaMarginPrevNext(ParaPr paraPr, Integer prevHwpUnit, Integer nextHwpUnit) {
        if (paraPr == null) return;
        if (paraPr.margin() == null) paraPr.createMargin();

        ParaMargin m = paraPr.margin();

        if (prevHwpUnit != null) {
            if (m.prev() == null) m.createPrev();
            m.prev().set(prevHwpUnit, ValueUnit2.HWPUNIT);
        }
        if (nextHwpUnit != null) {
            if (m.next() == null) m.createNext();
            m.next().set(nextHwpUnit, ValueUnit2.HWPUNIT);
        }
    }

    // 260307 추가
    private Map<String, Object> copySpec(Map<String, Object> spec) {
        if (spec == null) return null;
        return new HashMap<>(spec);
    }

    private int resolveHeaderRow(Map<String, Object> styleMap) {
        if (styleMap == null) return 0;

        Object hr = styleMap.get("headerRow");
        if (hr instanceof Number) return ((Number) hr).intValue();

        if (hr != null) {
            try {
                return Integer.parseInt(String.valueOf(hr).trim());
            } catch (Exception ignored) {}
        }

        return 0;
    }

    private boolean matchesVariationRule(
            Map<String, Object> rule,
            int rowIdx,
            int colIdx,
            int endRow,
            int endCol,
            Table table,
            int headerRow
    ) {
        if (rule == null || table == null) return false;

        boolean matched = true;
        boolean hasCondition = false;

        Object row = rule.get("row");
        if (row != null) {
            hasCondition = true;
            matched &= matchesIndexRule(row, rowIdx, endRow, table.getRowCount(), headerRow);
        }

        Object col = rule.get("col");
        if (col != null) {
            hasCondition = true;
            matched &= matchesIndexRule(col, colIdx, endCol, table.getColCount(), -1);
        }

        return hasCondition && matched;
    }

    private boolean matchesIndexRule(
            Object cond,
            int startIdx,
            int endIdx,
            int totalCount,
            int headerRow
    ) {
        if (cond == null) return false;

        if (cond instanceof Number) {
            return startIdx == ((Number) cond).intValue();
        }

        String s = String.valueOf(cond).trim().toLowerCase();
        if (s.isEmpty()) return false;

        switch (s) {
            case "first":
                return startIdx == 0;
            case "last":
                return endIdx == totalCount - 1;
            case "header":
                return startIdx == headerRow;
            case "body":
                return startIdx != headerRow;
            default:
                try {
                    return startIdx == Integer.parseInt(s);
                } catch (Exception ignored) {
                    return false;
                }
        }
    }

    private Map<String, Object> hiddenSpec() {
        Map<String, Object> m = new HashMap<>();
        m.put("style", "hidden");
        return m;
    }
}
