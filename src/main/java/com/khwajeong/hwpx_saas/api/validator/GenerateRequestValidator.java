package com.khwajeong.hwpx_saas.api.validator;

import com.khwajeong.hwpx_saas.api.dto.GenerateRequest;
import com.khwajeong.hwpx_saas.api.error.ApiException;
import com.khwajeong.hwpx_saas.api.error.ErrorCode;
import com.khwajeong.hwpx_saas.api.error.ValidationErrorDetail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GenerateRequestValidator {

  public static void validate(GenerateRequest req) {
    List<ValidationErrorDetail> errors = new ArrayList<>();

    if (req == null) {
      errors.add(err("request", "request body is required"));
      throwInvalidRequest(errors);
    }

    if (req.elements == null) {
      errors.add(err("elements", "elements is required"));
      throwInvalidRequest(errors);
    }

    if (req.elements.isEmpty()) {
      errors.add(err("elements", "elements must not be empty"));
      throwInvalidRequest(errors);
    }

    Set<String> placeholders = new HashSet<>();

    for (int i = 0; i < req.elements.size(); i++) {
      Map<String, Object> el = req.elements.get(i);
      String base = "elements[" + i + "]";

      if (el == null) {
        errors.add(err(base, "element must not be null"));
        continue;
      }

      String placeholder = str(el.get("placeholder"));
      String type = str(el.get("type"));

      if (placeholder.isBlank()) {
        errors.add(err(base + ".placeholder", "placeholder is required"));
      } else {
        if (!placeholders.add(placeholder)) {
          errors.add(err(base + ".placeholder", "placeholder must be unique: " + placeholder));
        }
      }

      if (type.isBlank()) {
        errors.add(err(base + ".type", "type is required"));
        continue;
      }

      if (!"text".equalsIgnoreCase(type) && !"table".equalsIgnoreCase(type)) {
        errors.add(err(base + ".type", "type must be either 'text' or 'table'"));
        continue;
      }

      if ("text".equalsIgnoreCase(type)) {
        validateTextElement(el, base, errors);
      }

      if ("table".equalsIgnoreCase(type)) {
        validateTableElement(el, base, errors);
      }
    }

    if (!errors.isEmpty()) {
      throwInvalidRequest(errors);
    }
  }

  private static void validateTextElement(
      Map<String, Object> el,
      String base,
      List<ValidationErrorDetail> errors
  ) {
    if (!el.containsKey("content")) {
      errors.add(err(base + ".content", "content is required for text type"));
    }

    Object styleObj = el.get("style");
    if (styleObj != null && !(styleObj instanceof Map<?, ?>)) {
      errors.add(err(base + ".style", "style must be an object"));
      return;
    }

    if (styleObj instanceof Map<?, ?> raw) {
      @SuppressWarnings("unchecked")
      Map<String, Object> style = (Map<String, Object>) raw;

      validateColor(style.get("color"), base + ".style.color", errors);
      validateColor(style.get("highlight"), base + ".style.highlight", errors);
      validateColor(style.get("backgroundColor"), base + ".style.backgroundColor", errors);

      validateInteger(style.get("font_size"), base + ".style.font_size", errors);
      validateInteger(style.get("fontSize"), base + ".style.fontSize", errors);
      validateInteger(style.get("space_before"), base + ".style.space_before", errors);
      validateInteger(style.get("space_after"), base + ".style.space_after", errors);

      validateAllowedValue(
          style.get("align"),
          base + ".style.align",
          errors,
          "left", "center", "centre", "right"
      );

      validateAllowedValue(
          style.get("script"),
          base + ".style.script",
          errors,
          "normal", "superscript", "subscript"
      );

      validateAllowedValue(
          style.get("multiline"),
          base + ".style.multiline",
          errors,
          "true", "false", "wrap", "nowrap"
      );
    }
  }

  private static void validateTableElement(
      Map<String, Object> el,
      String base,
      List<ValidationErrorDetail> errors
  ) {
    Object dataObj = el.get("data");
    if (!(dataObj instanceof List<?> rows)) {
      errors.add(err(base + ".data", "data is required for table type and must be a 2D array"));
      return;
    }

    if (rows.isEmpty()) {
      errors.add(err(base + ".data", "data must not be empty"));
      return;
    }

    int expectedCols = -1;

    for (int r = 0; r < rows.size(); r++) {
      Object rowObj = rows.get(r);
      if (!(rowObj instanceof List<?> row)) {
        errors.add(err(base + ".data[" + r + "]", "each row must be an array"));
        continue;
      }

      if (expectedCols == -1) {
        expectedCols = row.size();
        if (expectedCols == 0) {
          errors.add(err(base + ".data[0]", "row must not be empty"));
        }
      } else if (row.size() != expectedCols) {
        errors.add(err(
            base + ".data[" + r + "]",
            "all rows must have the same number of columns; expected " + expectedCols + " but got " + row.size()
        ));
      }
    }

    Object styleObj = el.get("style");
    if (styleObj != null && !(styleObj instanceof Map<?, ?>)) {
      errors.add(err(base + ".style", "style must be an object"));
      return;
    }

    if (styleObj instanceof Map<?, ?> raw) {
      @SuppressWarnings("unchecked")
      Map<String, Object> style = (Map<String, Object>) raw;

      validateInteger(style.get("headerRow"), base + ".style.headerRow", errors);
      validateBoolean(style.get("repeatHeader"), base + ".style.repeatHeader", errors);

      Object hr = style.get("headerRow");
      if (hr instanceof Number) {
        int headerRow = ((Number) hr).intValue();
        if (headerRow < -1 || headerRow >= rows.size()) {
          errors.add(err(
              base + ".style.headerRow",
              "headerRow is out of range; must be between -1 and " + (rows.size() - 1)
          ));
        }
      }

      Object cwObj = style.get("columnWidths");
      if (cwObj != null) {
        if (!(cwObj instanceof List<?> widths)) {
          errors.add(err(base + ".style.columnWidths", "columnWidths must be an array"));
        } else {
          if (expectedCols > -1 && widths.size() != expectedCols) {
            errors.add(err(
                base + ".style.columnWidths",
                "columnWidths size must match the number of columns; expected " + expectedCols + " but got " + widths.size()
            ));
          }

          for (int i = 0; i < widths.size(); i++) {
            if (!isNumericOrUnitString(widths.get(i))) {
              errors.add(err(
                  base + ".style.columnWidths[" + i + "]",
                  "each column width must be a number or a unit string like '20mm'"
              ));
            }
          }
        }
      }

      validateBorderStyle(style.get("border"), base + ".style.border", errors);
      validateBorderVariation(style.get("borderVariation"), base + ".style.borderVariation", errors);
      validateCellStyle(style.get("headerCell"), base + ".style.headerCell", errors);
      validateCellStyle(style.get("bodyCell"), base + ".style.bodyCell", errors);
      validateCellStyle(style.get("cell"), base + ".style.cell", errors);
    }

    Object mergeObj = el.get("merge_cells");
    if (mergeObj != null) {
      validateMergeCells(mergeObj, base + ".merge_cells", expectedCols, rows.size(), errors);
    }
  }

  private static void validateBorderStyle(
      Object borderObj,
      String base,
      List<ValidationErrorDetail> errors
  ) {
    if (borderObj == null) return;

    if (!(borderObj instanceof Map<?, ?> raw)) {
      errors.add(err(base, "border must be an object"));
      return;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> border = (Map<String, Object>) raw;

    validateBorderSpec(border.get("outer"), base + ".outer", errors);
    validateBorderSpec(border.get("inner"), base + ".inner", errors);

    Object headerObj = border.get("header");
    if (headerObj != null) {
      if (!(headerObj instanceof Map<?, ?> hRaw)) {
        errors.add(err(base + ".header", "header must be an object"));
      } else {
        @SuppressWarnings("unchecked")
        Map<String, Object> header = (Map<String, Object>) hRaw;
        validateBorderSpec(header.get("top"), base + ".header.top", errors);
        validateBorderSpec(header.get("bottom"), base + ".header.bottom", errors);
        validateBorderSpec(header.get("left"), base + ".header.left", errors);
        validateBorderSpec(header.get("right"), base + ".header.right", errors);
        validateBorderSpec(header.get("verticalInner"), base + ".header.verticalInner", errors);
        validateBorderSpec(header.get("horizontalInner"), base + ".header.horizontalInner", errors);
      }
    }

    Object lastRowObj = border.get("lastRow");
    if (lastRowObj != null) {
      if (!(lastRowObj instanceof Map<?, ?> lRaw)) {
        errors.add(err(base + ".lastRow", "lastRow must be an object"));
      } else {
        @SuppressWarnings("unchecked")
        Map<String, Object> lastRow = (Map<String, Object>) lRaw;
        validateBorderSpec(lastRow.get("top"), base + ".lastRow.top", errors);
        validateBorderSpec(lastRow.get("bottom"), base + ".lastRow.bottom", errors);
        validateBorderSpec(lastRow.get("left"), base + ".lastRow.left", errors);
        validateBorderSpec(lastRow.get("right"), base + ".lastRow.right", errors);
      }
    }
  }

  private static void validateBorderVariation(
      Object obj,
      String base,
      List<ValidationErrorDetail> errors
  ) {
    if (obj == null) return;

    if (!(obj instanceof List<?> items)) {
      errors.add(err(base, "borderVariation must be an array"));
      return;
    }

    for (int i = 0; i < items.size(); i++) {
      Object item = items.get(i);
      String itemBase = base + "[" + i + "]";

      if (!(item instanceof Map<?, ?> raw)) {
        errors.add(err(itemBase, "each borderVariation rule must be an object"));
        continue;
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> rule = (Map<String, Object>) raw;

      if (!rule.containsKey("row") && !rule.containsKey("col")) {
        errors.add(err(itemBase, "borderVariation rule must contain 'row' or 'col'"));
      }

      validateBorderSpec(rule.get("top"), itemBase + ".top", errors);
      validateBorderSpec(rule.get("bottom"), itemBase + ".bottom", errors);
      validateBorderSpec(rule.get("left"), itemBase + ".left", errors);
      validateBorderSpec(rule.get("right"), itemBase + ".right", errors);
      validateBorderSpec(rule.get("verticalInner"), itemBase + ".verticalInner", errors);
      validateBorderSpec(rule.get("horizontalInner"), itemBase + ".horizontalInner", errors);
    }
  }

  private static void validateCellStyle(
      Object obj,
      String base,
      List<ValidationErrorDetail> errors
  ) {
    if (obj == null) return;

    if (!(obj instanceof Map<?, ?> raw)) {
      errors.add(err(base, "cell style must be an object"));
      return;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> m = (Map<String, Object>) raw;

    validateColor(m.get("backgroundColor"), base + ".backgroundColor", errors);
    validateColor(m.get("background"), base + ".background", errors);
    validateColor(m.get("fillColor"), base + ".fillColor", errors);
    validateColor(m.get("bgColor"), base + ".bgColor", errors);
    validateColor(m.get("fontColor"), base + ".fontColor", errors);
    validateColor(m.get("color"), base + ".color", errors);

    validateInteger(m.get("fontSize"), base + ".fontSize", errors);
    validateInteger(m.get("font_size"), base + ".font_size", errors);

    Object paddingObj = m.get("padding");
    if (paddingObj != null) {
      if (!(paddingObj instanceof Map<?, ?> pRaw)) {
        errors.add(err(base + ".padding", "padding must be an object"));
      } else {
        @SuppressWarnings("unchecked")
        Map<String, Object> p = (Map<String, Object>) pRaw;
        validateNumericOrUnitString(p.get("left"), base + ".padding.left", errors);
        validateNumericOrUnitString(p.get("right"), base + ".padding.right", errors);
        validateNumericOrUnitString(p.get("top"), base + ".padding.top", errors);
        validateNumericOrUnitString(p.get("bottom"), base + ".padding.bottom", errors);
      }
    }

    Object textAlignObj = m.get("textAlign");
    if (textAlignObj != null) {
      if (!(textAlignObj instanceof Map<?, ?> tRaw)) {
        errors.add(err(base + ".textAlign", "textAlign must be an object"));
      } else {
        @SuppressWarnings("unchecked")
        Map<String, Object> t = (Map<String, Object>) tRaw;
        validateAllowedValue(
            t.get("horizontal"),
            base + ".textAlign.horizontal",
            errors,
            "left", "center", "centre", "middle", "right"
        );
        validateAllowedValue(
            t.get("vertical"),
            base + ".textAlign.vertical",
            errors,
            "top", "upper", "middle", "center", "centre", "bottom", "lower"
        );
      }
    }
  }

  private static void validateMergeCells(
      Object obj,
      String base,
      int colCount,
      int rowCount,
      List<ValidationErrorDetail> errors
  ) {
    if (!(obj instanceof List<?> items)) {
      errors.add(err(base, "merge_cells must be an array"));
      return;
    }

    for (int i = 0; i < items.size(); i++) {
      Object item = items.get(i);
      String itemBase = base + "[" + i + "]";

      if (!(item instanceof Map<?, ?> raw)) {
        errors.add(err(itemBase, "each merge_cells rule must be an object"));
        continue;
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> rule = (Map<String, Object>) raw;

      int[] start = readIndexPair(rule.get("start"));
      int[] end = readIndexPair(rule.get("end"));

      if (start == null) {
        errors.add(err(itemBase + ".start", "start must be [row, col]"));
        continue;
      }
      if (end == null) {
        errors.add(err(itemBase + ".end", "end must be [row, col]"));
        continue;
      }

      if (start[0] < 0 || start[0] >= rowCount || start[1] < 0 || start[1] >= colCount) {
        errors.add(err(itemBase + ".start", "start is out of table range"));
      }
      if (end[0] < 0 || end[0] >= rowCount || end[1] < 0 || end[1] >= colCount) {
        errors.add(err(itemBase + ".end", "end is out of table range"));
      }
      if (start[0] > end[0] || start[1] > end[1]) {
        errors.add(err(itemBase, "start must be before or equal to end"));
      }
    }
  }

  private static void validateBorderSpec(
      Object obj,
      String base,
      List<ValidationErrorDetail> errors
  ) {
    if (obj == null) return;

    if (!(obj instanceof Map<?, ?> raw)) {
      errors.add(err(base, "border spec must be an object"));
      return;
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> m = (Map<String, Object>) raw;

    Object style = m.get("style");
    if (style != null && str(style).isBlank()) {
      errors.add(err(base + ".style", "style must not be blank"));
    }

    validateNumericOrUnitString(m.get("width"), base + ".width", errors);
    validateColor(m.get("color"), base + ".color", errors);
  }

  private static void validateColor(
      Object value,
      String field,
      List<ValidationErrorDetail> errors
  ) {
    if (value == null) return;

    String s = str(value);
    if (s.isBlank()) return;

    if (s.startsWith("#")) s = s.substring(1);

    boolean ok = s.matches("^[0-9a-fA-F]{6}$") || s.matches("^[0-9a-fA-F]{3}$");
    if (!ok) {
      errors.add(err(field, "color must be a hex string like '#RRGGBB' or '#RGB'"));
    }
  }

  private static void validateInteger(
      Object value,
      String field,
      List<ValidationErrorDetail> errors
  ) {
    if (value == null) return;
    if (value instanceof Number) return;

    try {
      Integer.parseInt(str(value));
    } catch (Exception e) {
      errors.add(err(field, "must be an integer"));
    }
  }

  private static void validateBoolean(
      Object value,
      String field,
      List<ValidationErrorDetail> errors
  ) {
    if (value == null) return;
    if (value instanceof Boolean) return;

    String s = str(value).toLowerCase();
    if (!s.equals("true") && !s.equals("false")) {
      errors.add(err(field, "must be a boolean"));
    }
  }

  private static void validateAllowedValue(
      Object value,
      String field,
      List<ValidationErrorDetail> errors,
      String... allowed
  ) {
    if (value == null) return;

    String s = str(value).toLowerCase();
    if (s.isBlank()) return;

    for (String a : allowed) {
      if (a.equalsIgnoreCase(s)) return;
    }

    errors.add(err(field, "invalid value: " + value));
  }

  private static void validateNumericOrUnitString(
      Object value,
      String field,
      List<ValidationErrorDetail> errors
  ) {
    if (value == null) return;

    if (!isNumericOrUnitString(value)) {
      errors.add(err(field, "must be a number or a unit string like '20mm'"));
    }
  }

  private static boolean isNumericOrUnitString(Object value) {
    if (value == null) return false;
    if (value instanceof Number) return true;

    String s = str(value).toLowerCase();
    if (s.isBlank()) return false;

    return s.matches("^-?\\d+(\\.\\d+)?$") || s.matches("^-?\\d+(\\.\\d+)?mm$");
  }

  private static int[] readIndexPair(Object value) {
    if (!(value instanceof List<?> list) || list.size() != 2) return null;

    try {
      int a = Integer.parseInt(str(list.get(0)));
      int b = Integer.parseInt(str(list.get(1)));
      return new int[]{a, b};
    } catch (Exception e) {
      return null;
    }
  }

  private static ValidationErrorDetail err(String field, String message) {
    return new ValidationErrorDetail(field, message);
  }

  private static void throwInvalidRequest(List<ValidationErrorDetail> errors) {
    throw new ApiException(
        ErrorCode.INVALID_REQUEST,
        "generate request validation failed",
        errors
    );
  }

  private static String str(Object v) {
    return v == null ? "" : String.valueOf(v).trim();
  }
}