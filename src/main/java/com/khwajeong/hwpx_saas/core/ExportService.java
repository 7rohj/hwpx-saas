package com.khwajeong.hwpx_saas.core;

import io.github.mumberrymountain.HWPXTemplater;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {
  // -------------------------
  // helpers
  // -------------------------
  private boolean asBool(Object v, boolean def) {
    
    if (v == null) return def;
    if (v instanceof Boolean) return (Boolean) v;

    String s = String.valueOf(v).trim().toLowerCase();
    if (s.isEmpty()) return def;

    return s.equals("true") || s.equals("1") || s.equals("y") || s.equals("yes");
  }

  // CLI(byte[])용
  public byte[] renderHwpx(byte[] templateBytes, Map<String, Object> request) throws Exception {
    System.out.println("[WHICH] StyleRenderer from: " +
        io.github.mumberrymountain.render.style.StyleRenderer.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation());
                
    Path tmp = Files.createTempFile("template-", ".hwpx");
    try {
      Files.write(tmp, templateBytes);

      Map<String, Object> renderData = convertRequestToRenderData(request);

      for (Map.Entry<String, Object> e : renderData.entrySet()) {
          Object v = e.getValue();
          System.out.println("[RENDER_DATA] key=" + e.getKey()
              + ", class=" + (v == null ? "null" : v.getClass().getName())
              + ", value=" + v);
      }

      System.out.println("[WHICH] HWPXTemplater = " +
          io.github.mumberrymountain.HWPXTemplater.class.getProtectionDomain().getCodeSource().getLocation());

      System.out.println("[WHICH] TableRenderer = " +
          io.github.mumberrymountain.render.TableRenderer.class.getProtectionDomain().getCodeSource().getLocation());

      System.out.println("[WHICH] Table model = " +
          io.github.mumberrymountain.model.table.Table.class.getProtectionDomain().getCodeSource().getLocation());

      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HWPXTemplater.builder()
            .config("delim.start", "{{")
            .config("delim.end", "}}")
            .parse(tmp.toString())
            .render(renderData)
            .write(baos);

        return baos.toByteArray();

      } catch (Exception e) {
        System.out.println("=== [EXPORT ERROR] renderHwpx(byte[]) 실패 ===");
        e.printStackTrace();
        throw e;
      }

    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  // REST(MultipartFile)용
  public byte[] renderHwpx(MultipartFile template, Map<String, Object> request) throws Exception {
    Path tmp = Files.createTempFile("template-", ".hwpx");
    try {
      template.transferTo(tmp);

      Map<String, Object> renderData = convertRequestToRenderData(request);

      for (Map.Entry<String, Object> e : renderData.entrySet()) {
          Object v = e.getValue();
          System.out.println("[RENDER_DATA] key=" + e.getKey()
              + ", class=" + (v == null ? "null" : v.getClass().getName())
              + ", value=" + v);
      }

      System.out.println("[WHICH] HWPXTemplater = " +
          io.github.mumberrymountain.HWPXTemplater.class.getProtectionDomain().getCodeSource().getLocation());

      System.out.println("[WHICH] TableRenderer = " +
          io.github.mumberrymountain.render.TableRenderer.class.getProtectionDomain().getCodeSource().getLocation());

      System.out.println("[WHICH] Table model = " +
          io.github.mumberrymountain.model.table.Table.class.getProtectionDomain().getCodeSource().getLocation());

      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HWPXTemplater.builder()
            .config("delim.start", "{{")
            .config("delim.end", "}}")
            .parse(tmp.toString())
            .render(renderData)
            .write(baos);

        return baos.toByteArray();

      } catch (Exception e) {
        System.out.println("=== [EXPORT ERROR] renderHwpx(MultipartFile) 실패 ===");
        e.printStackTrace();
        throw e;
      }

    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  private Map<String, Object> convertRequestToRenderData(Map<String, Object> request) {
    Object elementsObj = request.get("elements");
    if (!(elementsObj instanceof List<?> elements)) {
      throw new IllegalArgumentException("Invalid request: 'elements' must be a JSON array.");
    }
    return convertElements(elements, request.get("global_variables"));
  }

  private Map<String, Object> convertElements(List<?> elements, Object globalVarsObj) {
    Map<String, Object> renderData = new HashMap<>();

    // 1) global_variables merge ({{author}}, {{date}} 등)
    if (globalVarsObj instanceof Map<?, ?> gv) {
      for (var e : gv.entrySet()) {
        String k = String.valueOf(e.getKey());
        if (k != null && !k.isBlank()) {
          renderData.put(k, e.getValue());
        }
      }
    }

    // 2) elements 처리
    for (Object el : elements) {
      if (!(el instanceof Map<?, ?> m)) continue;

      String placeholder = m.get("placeholder") == null ? "" : String.valueOf(m.get("placeholder")).trim();
      String type = m.get("type") == null ? "" : String.valueOf(m.get("type")).trim();

      if (placeholder.isBlank() || type.isBlank()) continue;

      // -------------------------
      // TEXT
      // -------------------------
      if ("text".equalsIgnoreCase(type)) {
        Object contentObj = m.get("content");
        String content = contentObj == null ? "" : String.valueOf(contentObj);

        Object styleObj = m.get("style");
        if (styleObj instanceof Map<?, ?> styleMap) {
          io.github.mumberrymountain.model.Text txt = new io.github.mumberrymountain.model.Text(content);

          // font_family
          Object ff = styleMap.get("font_family");
          if (ff != null && !String.valueOf(ff).isBlank()) {
            txt.fontFamily(String.valueOf(ff).trim());
          }

          // font_size (pt)
          Object fs = styleMap.get("font_size");
          if (fs instanceof Number) {
            txt.fontSize(((Number) fs).intValue());
          } else if (fs != null) {
            try { txt.fontSize(Integer.parseInt(String.valueOf(fs).trim())); } catch (Exception ignored) {}
          }

          // bold/italic/underline/strike
          txt.bold(asBool(styleMap.get("bold"), false));
          txt.italic(asBool(styleMap.get("italic"), false));
          txt.underLine(asBool(styleMap.get("underline"), false));
          txt.strikeOut(asBool(styleMap.get("strike"), false));

          // font color
          Object color = styleMap.get("color");
          if (color != null && !String.valueOf(color).isBlank()) {
            txt.fontColor(String.valueOf(color).trim());   // "#RRGGBB" 형태 기대
          }

          // highlight/background
          Object bg = styleMap.get("highlight"); // 형광펜
          if (bg == null) bg = styleMap.get("backgroundColor"); // 호환
          if (bg != null && !String.valueOf(bg).isBlank()) {
            txt.backgroundColor(String.valueOf(bg).trim());
          }

          // (확장용) align/script/multiline: Text 모델에 필드 추가했으니 여기서 저장 가능
          Object al = styleMap.get("align");
          if (al != null && !String.valueOf(al).isBlank()) txt.align(String.valueOf(al).trim());

          Object sc = styleMap.get("script");
          if (sc != null && !String.valueOf(sc).isBlank()) txt.script(String.valueOf(sc).trim());

          Object ml = styleMap.get("multiline");
          if (ml != null && !String.valueOf(ml).isBlank()) txt.multiline(String.valueOf(ml).trim());

          // =========================
          // MOD 2026-02-25: line_spacing 지원 추가
          // - data.json 예시1) "line_spacing": 160
          // - data.json 예시2) "line_spacing": {"type":"percent","value":160}
          // =========================
          Object lsObj = styleMap.get("line_spacing");
          if (lsObj instanceof Map<?, ?> lsMap) {
            Object t = lsMap.get("type");
            Object v = lsMap.get("value");

            String typeStr = null;
            Integer valInt = null;

            if (t != null && !String.valueOf(t).isBlank()) {
              typeStr = String.valueOf(t).trim();
              txt.lineSpacingType(typeStr);
            }

            if (v instanceof Number) {
              valInt = ((Number) v).intValue();
              txt.lineSpacingValue(valInt);
            } else if (v != null) {
              try {
                valInt = Integer.parseInt(String.valueOf(v).trim());
                txt.lineSpacingValue(valInt);
              } catch (Exception ignored) {}
            }

            // percent shortcut도 같이 채움(렌더러에서 편하게 쓰기 위함)
            if (typeStr != null && "percent".equalsIgnoreCase(typeStr) && valInt != null) {
              txt.lineSpacingPercent(valInt);
            }
          } else if (lsObj instanceof Number) {
            int pct = ((Number) lsObj).intValue();
            txt.lineSpacingType("percent");
            txt.lineSpacingValue(pct);
            txt.lineSpacingPercent(pct); // percent shortcut
          }
          // =========================

          // =========================
          // MOD 2026-02-27: space_before / space_after 지원 추가
          // - data.json 예시) "space_before": 200, "space_after": 200
          // =========================
          Object sb = styleMap.get("space_before");
          if (sb instanceof Number) {
            txt.spaceBefore(((Number) sb).intValue());
          } else if (sb != null) {
            try { txt.spaceBefore(Integer.parseInt(String.valueOf(sb).trim())); } catch (Exception ignored) {}
          }

          Object sa = styleMap.get("space_after");
          if (sa instanceof Number) {
            txt.spaceAfter(((Number) sa).intValue());
          } else if (sa != null) {
            try { txt.spaceAfter(Integer.parseInt(String.valueOf(sa).trim())); } catch (Exception ignored) {}
          }
          // =========================

          renderData.put(placeholder, txt);
        } else {
          // style 없으면 기존대로 (String)
          renderData.put(placeholder, content);
        }
        continue;
      }

      // -------------------------
      // TABLE
      // -------------------------
      if ("table".equalsIgnoreCase(type)) {
        Object dataObj = m.get("data"); // 신규포맷은 data

        if (!(dataObj instanceof List<?> rows) || rows.isEmpty() || !(rows.get(0) instanceof List<?>)) {
          renderData.put(placeholder, TableMapper.fromGridWithHeaderAndStyle(List.of(List.of("")), -1, false, null));
          continue;
        }

        @SuppressWarnings("unchecked")
        List<List<?>> grid = (List<List<?>>) dataObj;

        // style 파싱
        int headerRow = -1;
        boolean repeatHeader = false;
        Map<String, Object> styleMap = null;

        Object styleObj = m.get("style");
        if (styleObj instanceof Map<?, ?> style) {
          styleMap = new HashMap<>();
          for (var e : style.entrySet()) {
            styleMap.put(String.valueOf(e.getKey()), e.getValue());
          }

          Object hr = style.get("headerRow");
          Object rh = style.get("repeatHeader");
          if (hr instanceof Number) headerRow = ((Number) hr).intValue();
          if (rh instanceof Boolean) repeatHeader = (Boolean) rh;
        }

        // merge_cells를 styleMap에 합치기
        Object mergeObj = m.get("merge_cells");
        if (mergeObj != null) {
          if (styleMap == null) styleMap = new HashMap<>();
          styleMap.put("merge_cells", mergeObj);
        }

        renderData.put(
            placeholder,
            TableMapper.fromGridWithHeaderAndStyle(grid, headerRow, repeatHeader, styleMap)
        );
        continue;
      }

      // fallback
      Object content = m.get("content");
      renderData.put(placeholder, content == null ? "" : String.valueOf(content));
    }

    return renderData;
  }
}
