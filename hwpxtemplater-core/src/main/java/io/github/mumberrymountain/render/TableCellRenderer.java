package io.github.mumberrymountain.render;

import io.github.mumberrymountain.model.table.*;
import io.github.mumberrymountain.util.HWPXUnitUtil;
import kr.dogfoot.hwpxlib.object.content.section_xml.SubList;
import kr.dogfoot.hwpxlib.object.content.section_xml.enumtype.LineWrapMethod;
import kr.dogfoot.hwpxlib.object.content.section_xml.enumtype.TextDirection;
import kr.dogfoot.hwpxlib.object.content.section_xml.enumtype.VerticalAlign2;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.T;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tc;

import java.util.Map;

public class TableCellRenderer {

    private final HWPXRenderer rootRenderer;
    private final Tc renderingCell;
    private final int rowIdx;
    private final int colIdx;
    private final Table tableParam;
    private final Cell cell;
    private final Col col;
    private final Row row;

    public TableCellRenderer(HWPXRenderer rootRenderer, Tc renderingCell, int rowIdx, int colIdx, Table tableParam) {
        this.rootRenderer = rootRenderer;
        this.renderingCell = renderingCell;
        this.rowIdx = rowIdx;
        this.colIdx = colIdx;
        this.tableParam = tableParam;
        this.cell = tableParam.getCell(rowIdx, colIdx);
        this.row = tableParam.getRow(rowIdx);
        this.col = tableParam.getCol(colIdx);
    }

    private void setTc() {
        renderingCell.name("");

        boolean isHeader = (row != null && row.getRowType() == RowType.Header);
        renderingCell.header(isHeader);

        // 핵심: headerCell/bodyCell(없으면 cell) padding이 있으면 "안 여백 지정" 토글 ON
        boolean hasCellPadding = (getPaddingMapFromCellStyle() != null);
        renderingCell.hasMargin(hasCellPadding);

        renderingCell.protect(false);
        renderingCell.editable(false);
        renderingCell.dirty(false);

        // table + 좌표 넘겨서 outer/inner를 면별로 적용
        renderingCell.borderFillIDRef(
                rootRenderer.styleRenderer().renderBorderStyle(cell, tableParam, rowIdx, colIdx, resolveBackgroundColor())
        );
        
    }

    private String resolveBackgroundColor() {
        Map<String, Object> cellStyle = getCellStyleMapForCurrentRow();
        if (cellStyle == null) return null;

        Object bg = cellStyle.get("backgroundColor");
        if (bg == null) bg = cellStyle.get("background");
        if (bg == null) bg = cellStyle.get("fillColor");
        if (bg == null) bg = cellStyle.get("bgColor");
        if (bg == null) return null;

        String s = String.valueOf(bg).trim();
        return s.isEmpty() ? null : s;
    }

    private void setCellAddr() {
        renderingCell.createCellAddr();
        renderingCell.cellAddr().colAddr((short) colIdx);
        renderingCell.cellAddr().rowAddr((short) rowIdx);
    }

    private void setCellSpan() {
        renderingCell.createCellSpan();

        if (isCovered(cell)) {
            renderingCell.cellSpan().colSpan((short) 0);
            renderingCell.cellSpan().rowSpan((short) 0);
            return;
        }

        int cs = Math.max(reflectInt(cell, "getColSpan", 1), 1);
        int rs = Math.max(reflectInt(cell, "getRowSpan", 1), 1);

        renderingCell.cellSpan().colSpan((short) cs);
        renderingCell.cellSpan().rowSpan((short) rs);
    }

    private void setCellSz() {
        renderingCell.createCellSz();

        if (isCovered(cell)) {
            renderingCell.cellSz().width(0L);
            renderingCell.cellSz().height(0L);
            return;
        }

        int cs = Math.max(reflectInt(cell, "getColSpan", 1), 1);
        int rs = Math.max(reflectInt(cell, "getRowSpan", 1), 1);

        int widthPx = 0;
        for (int c = colIdx; c < colIdx + cs; c++) {
            Col cObj = tableParam.getCol(c);
            if (cObj != null) widthPx += cObj.getWidth();
        }

        int heightPx = 0;
        for (int r = rowIdx; r < rowIdx + rs; r++) {
            Row rObj = tableParam.getRow(r);
            if (rObj != null) heightPx += rObj.getHeight();
        }

        renderingCell.cellSz().width((long) HWPXUnitUtil.pxToHwpxUnit(widthPx));
        renderingCell.cellSz().height((long) HWPXUnitUtil.pxToHwpxUnit(heightPx));
    }

    private void setCellMargin() {
        renderingCell.createCellMargin();

        if (isCovered(cell)) {
            renderingCell.cellMargin().left(0L);
            renderingCell.cellMargin().right(0L);
            renderingCell.cellMargin().top(0L);
            renderingCell.cellMargin().bottom(0L);
            return;
        }

        // style.headerCell/bodyCell.padding (없으면 style.cell.padding)
        Map<String, Object> padding = getPaddingMapFromCellStyle();

        // 기본값(mm) - 필요하면 조정
        long left   = asHwpxFromMmOrDefault(padding, "left",   1.8);
        long right  = asHwpxFromMmOrDefault(padding, "right",  1.8);
        long top    = asHwpxFromMmOrDefault(padding, "top",    0.5);
        long bottom = asHwpxFromMmOrDefault(padding, "bottom", 0.5);

        renderingCell.cellMargin().left(left);
        renderingCell.cellMargin().right(right);
        renderingCell.cellMargin().top(top);
        renderingCell.cellMargin().bottom(bottom);
    }

    private SubList cellSubListDef() {
        renderingCell.createSubList();
        SubList sl = renderingCell.subList();
        sl.id("");
        sl.textDirection(TextDirection.HORIZONTAL);
        sl.lineWrap(LineWrapMethod.BREAK);

        sl.vertAlign(resolveVerticalAlignFromStyle()); // header/body 분기 반영

        sl.linkListIDRef("0");
        sl.linkListNextIDRef("0");
        sl.textWidth(0);
        sl.textHeight(0);
        sl.hasTextRef(false);
        sl.hasNumRef(false);
        return sl;
    }

    private void setSubList() {
        setParagraph(cellSubListDef());
    }

    // header/body textAlign.horizontal 우선 반영
    private Align resolveHorizontalAlign(Cell cell, Col col) {
        Map<String, Object> ta = getTextAlignMapFromCellStyle();
        if (ta != null) {
            Object hObj = ta.get("horizontal");
            if (hObj != null) {
                String h = String.valueOf(hObj).trim().toLowerCase();
                switch (h) {
                    case "left":
                        return Align.Left;
                    case "center":
                    case "centre":
                    case "middle":
                        return Align.Center;
                    case "right":
                        return Align.Right;
                    default:
                        break;
                }
            }
        }

        // fallback: 기존 로직
        if (cell != null && cell.getAlign() != null) return cell.getAlign();
        if (col != null && col.getAlign() != null) return col.getAlign();
        return Align.Left;
    }

    private Para cellParaDef(SubList sl) {
        Para cellPara = sl.addNewPara();
        cellPara.id("0");
        cellPara.paraPrIDRef(
                rootRenderer.styleRenderer()
                        .renderParaStyleAndReturnParaPrId(resolveHorizontalAlign(cell, col))
        );
        cellPara.styleIDRef("0");
        cellPara.pageBreak(false);
        cellPara.columnBreak(false);
        cellPara.merged(false);
        return cellPara;
    }

    private void setParagraph(SubList sl) {
        setRun(cellParaDef(sl));
    }

    private void setRun(Para cellPara) {
        Run cellRun = cellPara.addNewRun();
        T cellT = cellRun.addNewT();

        io.github.mumberrymountain.model.Text text =
                (cell == null || cell.getText() == null)
                        ? new io.github.mumberrymountain.model.Text("")
                        : cell.getText();

        String val = text.getValue();
        if (val == null) val = "";
        if (RendererUtil.isAutoTrim(rootRenderer.config())) val = val.trim();

        applyTextStyleFromCellStyle(text);

        cellT.addText(val);
        cellRun.charPrIDRef(rootRenderer.styleRenderer().renderCharStyleAndReturnCharPrId(text));
    }

    public void render() {
        setTc();

        if (!isCovered(cell)) {
            setSubList();
        }

        setCellAddr();
        setCellSpan();
        setCellSz();
        setCellMargin();
    }

    // -------------------------
    // style helpers
    // - style.headerCell / style.bodyCell 우선
    // - 없으면 style.cell (하위호환)
    // -------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> getStyleMap() {
        if (tableParam == null) return null;
        Object styleObj = tableParam.getConfig("style");
        if (styleObj instanceof Map) return (Map<String, Object>) styleObj;
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getCellStyleMapForCurrentRow() {
        Map<String, Object> style = getStyleMap();
        if (style == null) return null;

        boolean isHeader = (row != null && row.getRowType() == RowType.Header);
        Object obj = isHeader ? style.get("headerCell") : style.get("bodyCell");
        if (obj instanceof Map) return (Map<String, Object>) obj;

        // 하위호환: style.cell
        Object legacy = style.get("cell");
        if (legacy instanceof Map) return (Map<String, Object>) legacy;

        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPaddingMapFromCellStyle() {
        Map<String, Object> cellStyle = getCellStyleMapForCurrentRow();
        if (cellStyle == null) return null;

        Object paddingObj = cellStyle.get("padding");
        if (!(paddingObj instanceof Map)) return null;

        return (Map<String, Object>) paddingObj;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getTextAlignMapFromCellStyle() {
        Map<String, Object> cellStyle = getCellStyleMapForCurrentRow();
        if (cellStyle == null) return null;

        Object taObj = cellStyle.get("textAlign");
        if (!(taObj instanceof Map)) return null;

        return (Map<String, Object>) taObj;
    }

    public static long mmToHwpxUnit(double mm) {
        // HWPX 단위: 1/7200 inch (dogfoot hwpx에서 width/height/margin에 쓰는 단위)
        // 1 inch = 25.4 mm
        // => hwpxUnit = mm / 25.4 * 7200
        return Math.round(mm * 7200.0 / 25.4);
    }

    private long asHwpxFromMmOrDefault(Map<String, Object> m, String key, double defMm) {
        double mm = defMm;
        if (m != null) {
            Double parsed = tryParseDouble(m.get(key));
            if (parsed != null) mm = parsed;
        }
        return Math.round(mm * 7200.0 / 25.4);
    }

    private Double tryParseDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            String s = String.valueOf(v).trim().toLowerCase();
            if (s.endsWith("mm")) s = s.substring(0, s.length() - 2).trim();
            if (s.isEmpty()) return null;
            return Double.parseDouble(s);
        } catch (Exception ignored) {
        }
        return null;
    }

    // -------------------------
    // reflection helpers
    // -------------------------

    private boolean isCovered(Object cellObj) {
        if (cellObj == null) return false;
        try {
            java.lang.reflect.Method m = cellObj.getClass().getMethod("isCovered");
            Object r = m.invoke(cellObj);
            return (r instanceof Boolean) && (Boolean) r;
        } catch (Exception ignored) {
        }
        return false;
    }

    private int reflectInt(Object target, String method, int fallback) {
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(method);
            Object r = m.invoke(target);
            if (r instanceof Number) return ((Number) r).intValue();
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private VerticalAlign2 resolveVerticalAlignFromStyle() {
        // default = 가운데(기존 동작 유지)
        Map<String, Object> ta = getTextAlignMapFromCellStyle();
        if (ta == null) return VerticalAlign2.CENTER;

        Object vObj = ta.get("vertical");
        if (vObj == null) return VerticalAlign2.CENTER;

        String v = String.valueOf(vObj).trim().toLowerCase();
        if (v.isEmpty()) return VerticalAlign2.CENTER;

        switch (v) {
            case "top":
            case "upper":
                return VerticalAlign2.TOP;

            case "middle":
            case "center":
            case "centre":
                return VerticalAlign2.CENTER;

            case "bottom":
            case "lower":
                return VerticalAlign2.BOTTOM;

            default:
                return VerticalAlign2.CENTER;
        }
    }

    private void applyTextStyleFromCellStyle(io.github.mumberrymountain.model.Text text) {
        Map<String, Object> cellStyle = getCellStyleMapForCurrentRow();
        if (cellStyle == null || text == null) return;

        Object ff = cellStyle.get("fontFamily");
        if (ff == null) ff = cellStyle.get("font_family");
        if (ff != null && !String.valueOf(ff).isBlank()) {
            text.fontFamily(String.valueOf(ff).trim());
        }

        Object fs = cellStyle.get("fontSize");
        if (fs == null) fs = cellStyle.get("font_size");
        if (fs instanceof Number) {
            text.fontSize(((Number) fs).intValue());
        } else if (fs != null) {
            try { text.fontSize(Integer.parseInt(String.valueOf(fs).trim())); } catch (Exception ignored) {}
        }

        Object fc = cellStyle.get("fontColor");
        if (fc == null) fc = cellStyle.get("color");
        if (fc != null && !String.valueOf(fc).isBlank()) {
            text.fontColor(String.valueOf(fc).trim());
        }

        Object bold = cellStyle.get("bold");
        if (bold != null) {
            text.bold(asBool(bold, false));
        }

        Object italic = cellStyle.get("italic");
        if (italic != null) {
            text.italic(asBool(italic, false));
        }

        Object underline = cellStyle.get("underline");
        if (underline != null) {
            text.underLine(asBool(underline, false));
        }

        Object strike = cellStyle.get("strike");
        if (strike != null) {
            text.strikeOut(asBool(strike, false));
        }
    }

    private boolean asBool(Object v, boolean def) {
        if (v == null) return def;
        if (v instanceof Boolean) return (Boolean) v;

        String s = String.valueOf(v).trim().toLowerCase();
        if (s.isEmpty()) return def;

        return s.equals("true") || s.equals("1") || s.equals("y") || s.equals("yes");
    }
}
