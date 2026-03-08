package com.khwajeong.hwpx_saas.core;

import io.github.mumberrymountain.model.table.Cell;
import io.github.mumberrymountain.model.table.Col;
import io.github.mumberrymountain.model.table.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TableMapper {
  private TableMapper() {}

  public static Table from2D(List<List<?>> grid) {
    return fromGridWithHeaderAndStyle(grid, -1, true, null);
  }

  public static Table fromGridWithHeader(List<List<?>> grid, int headerRow, boolean repeatHeader) {
    return fromGridWithHeaderAndStyle(grid, headerRow, repeatHeader, null);
  }

  /**
   * style 지원
   * - style.columnWidths: [140,40,40] / ["120"] / ["20mm"] 등
   * - style.merge_cells: [{"start":[3,0],"end":[3,1]}]  // 0-based
   */
  public static Table fromGridWithHeaderAndStyle(
      List<List<?>> grid,
      int headerRow,
      boolean repeatHeader,
      Map<String, Object> style
  ) {

    // 빈 그리드 방어
    if (grid == null || grid.isEmpty() || grid.get(0) == null || grid.get(0).isEmpty()) {
      grid = new ArrayList<>();
      List<String> one = new ArrayList<>();
      one.add("");
      grid.add(one);
    }

    final int rows = grid.size();
    final int cols = grid.get(0).size();

    // 모든 행 col 수 동일 체크
    for (int r = 0; r < rows; r++) {
      List<?> row = grid.get(r);
      if (row == null || row.size() != cols) {
        throw new IllegalArgumentException("All rows must have same column count");
      }
    }

    // style 파싱 (columnWidths)
    List<Integer> colWidthsInt = extractColumnWidthInts(style, cols);

    // 컬럼 정의 (Col.width 결정)
    List<Col> colDefs = new ArrayList<>();
    for (int c = 0; c < cols; c++) {
      int width = 210; // 기본값(원하는 값으로)
      if (colWidthsInt != null) {
        Integer w = colWidthsInt.get(c);
        if (w != null && w.intValue() > 0) width = w.intValue();
      }
      colDefs.add(new Col("C" + (c + 1)).width(width));
    }

    // Table.builder
    Table.builder b = Table.builder()
        .cols(colDefs)
        .config("repeatHeader", repeatHeader);

    // style 저장 (디버깅/확장용)
    if (style != null && !style.isEmpty()) {
      b.config("style", style);

      Object tableWidth = style.get("tableWidth");
      if (tableWidth != null) b.config("tableWidth", normalizeToString(tableWidth));

      Object rowHeight = style.get("rowHeight");
      if (rowHeight != null) b.config("rowHeight", rowHeight);

      Object border = style.get("border");
      if (border instanceof Map) {
        b.config("border", border);
      }
    }

    // row 만들기
    for (int r = 0; r < rows; r++) {
      HashMap<String, Object> rowMap = new HashMap<>();
      for (int c = 0; c < cols; c++) {
        Object v = grid.get(r).get(c);
        rowMap.put("C" + (c + 1), v == null ? "" : String.valueOf(v));
      }

      if (r == headerRow) b.headerRow(rowMap);
      else b.row(rowMap);
    }

    // ✅ Table 생성
    Table table = b.create();

    // ✅ merge_cells 적용 (style 안에 merge_cells가 들어있다는 전제)
    List<MergeCell> merges = extractMergeCells(style);
    if (merges != null && !merges.isEmpty()) {
      applyMergesToTable(table, rows, cols, merges);
    }

    return table;
  }

  private static String normalizeToString(Object o) {
    if (o == null) return null;
    return String.valueOf(o).trim();
  }

  // -------------------------
  // Parsing helpers: columnWidths
  // -------------------------

  private static List<Integer> extractColumnWidthInts(Map<String, Object> style, int cols) {
    if (style == null) return null;
    Object obj = style.get("columnWidths");
    if (!(obj instanceof List)) return null;

    List<?> list = (List<?>) obj;
    if (list.isEmpty()) return null;

    List<Integer> out = new ArrayList<>();
    for (Object w : list) out.add(parseWidthToIntIfPossible(w));

    return fitToCols(out, cols);
  }

  private static Integer parseWidthToIntIfPossible(Object w) {
    if (w == null) return null;

    if (w instanceof Number) return ((Number) w).intValue();

    String s = String.valueOf(w).trim().toLowerCase();
    if (s.isEmpty()) return null;
    if ("auto".equals(s)) return null;

    // percent는 지금 버전에서 즉시 적용 안함
    if (s.endsWith("%")) return null;

    // "20mm" -> 20 (임시)
    if (s.endsWith("mm")) {
      String num = s.substring(0, s.length() - 2).trim();
      Double d = tryParseDouble(num);
      if (d == null) return null;
      return (int) Math.round(d.doubleValue());
    }

    Double d = tryParseDouble(s);
    if (d == null) return null;
    return (int) Math.round(d.doubleValue());
  }

  private static Double tryParseDouble(String s) {
    try {
      return Double.valueOf(s);
    } catch (Exception e) {
      return null;
    }
  }

  private static List<Integer> fitToCols(List<Integer> in, int cols) {
    if (in == null) return null;
    if (cols <= 0) return in;

    List<Integer> out = new ArrayList<>();
    int n = Math.min(in.size(), cols);
    for (int i = 0; i < n; i++) out.add(in.get(i));

    Integer last = out.isEmpty() ? null : out.get(out.size() - 1);
    while (out.size() < cols) out.add(last);

    return out;
  }

  // -------------------------
  // Merge Cells
  // -------------------------

  private static final class MergeCell {
    final int sr, sc, er, ec;
    MergeCell(int sr, int sc, int er, int ec) {
      this.sr = sr;
      this.sc = sc;
      this.er = er;
      this.ec = ec;
    }
  }

  /**
   * style.merge_cells:
   * [
   *   {"start":[3,0], "end":[3,1]}
   * ]
   */
  @SuppressWarnings("unchecked")
  private static List<MergeCell> extractMergeCells(Map<String, Object> style) {
    if (style == null) return null;

    Object obj = style.get("merge_cells");
    if (!(obj instanceof List)) return null;

    List<?> list = (List<?>) obj;
    if (list.isEmpty()) return null;

    List<MergeCell> out = new ArrayList<>();

    for (Object item : list) {
      if (!(item instanceof Map)) continue;
      Map<String, Object> m = (Map<String, Object>) item;

      Object startObj = m.get("start");
      Object endObj = m.get("end");
      if (!(startObj instanceof List) || !(endObj instanceof List)) continue;

      List<?> start = (List<?>) startObj;
      List<?> end = (List<?>) endObj;
      if (start.size() < 2 || end.size() < 2) continue;

      Object srObj = start.get(0);
      Object scObj = start.get(1);
      Object erObj = end.get(0);
      Object ecObj = end.get(1);

      if (!(srObj instanceof Number) || !(scObj instanceof Number) ||
          !(erObj instanceof Number) || !(ecObj instanceof Number)) {
        continue;
      }

      int sr = ((Number) srObj).intValue();
      int sc = ((Number) scObj).intValue();
      int er = ((Number) erObj).intValue();
      int ec = ((Number) ecObj).intValue();

      out.add(new MergeCell(sr, sc, er, ec));
    }

    return out.isEmpty() ? null : out;
  }

  /**
   * Table 내부 Cell에
   * - 시작 셀: rowSpan/colSpan > 1
   * - 병합에 포함된 나머지 셀: covered=true, rowSpan=0, colSpan=0
   */
  private static void applyMergesToTable(Table table, int rowCount, int colCount, List<MergeCell> merges) {
    // 기본 초기화 (Cell 기본값이 이미 1/1/false라면 없어도 되지만 안전하게 한 번)
    for (int r = 0; r < rowCount; r++) {
      for (int c = 0; c < colCount; c++) {
        Cell cell = table.getCell(r, c);
        if (cell != null) {
          cell.rowSpan(1).colSpan(1).covered(false);
        }
      }
    }

    for (MergeCell m : merges) {
      int sr = m.sr, sc = m.sc, er = m.er, ec = m.ec;

      // 범위 방어
      if (sr < 0 || sc < 0 || er < 0 || ec < 0) continue;
      if (sr >= rowCount || er >= rowCount || sc >= colCount || ec >= colCount) continue;
      if (er < sr || ec < sc) continue;

      int rs = er - sr + 1;
      int cs = ec - sc + 1;

      // 시작 셀
      Cell start = table.getCell(sr, sc);
      if (start != null) {
        start.rowSpan(rs).colSpan(cs).covered(false);
      }

      // 나머지 covered
      for (int r = sr; r <= er; r++) {
        for (int c = sc; c <= ec; c++) {
          if (r == sr && c == sc) continue;
          Cell cc = table.getCell(r, c);
          if (cc != null) {
            cc.rowSpan(0).colSpan(0).covered(true);
          }
        }
      }
    }
  }
}
