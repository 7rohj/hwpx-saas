package io.github.mumberrymountain.render.style;

import kr.dogfoot.hwpxlib.object.content.header_xml.HeaderXMLFile;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.LineType2;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.LineWidth;
import kr.dogfoot.hwpxlib.object.content.header_xml.enumtype.SlashType;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.BorderFill;
import kr.dogfoot.hwpxlib.object.content.header_xml.references.borderfill.FillBrush;
import io.github.mumberrymountain.model.table.Cell;

import java.util.Map;

public class BorderRenderer {
    private final HeaderXMLFile headerXMLFile;
    private final Cell cell;
    private final BorderFill borderFill;

    // 상/하/좌/우 spec
    private final Map<String, Object> leftSpec;
    private final Map<String, Object> rightSpec;
    private final Map<String, Object> topSpec;
    private final Map<String, Object> bottomSpec;

    // 기존 생성자(호환)
    public BorderRenderer(HeaderXMLFile headerXMLFile, Cell cell){
        this(headerXMLFile, cell, null, null, null, null);
    }

    // 신규 생성자 (상/하/좌/우)
    public BorderRenderer(
            HeaderXMLFile headerXMLFile,
            Cell cell,
            Map<String, Object> leftSpec,
            Map<String, Object> rightSpec,
            Map<String, Object> topSpec,
            Map<String, Object> bottomSpec
    ){
        this.headerXMLFile = headerXMLFile;
        this.cell = cell;
        this.borderFill = new BorderFill();

        this.leftSpec = leftSpec;
        this.rightSpec = rightSpec;
        this.topSpec = topSpec;
        this.bottomSpec = bottomSpec;
    }

    private void setId(){
        String bfId = Integer.toString(headerXMLFile.refList().borderFills().count() + 1);
        borderFill.id(bfId);
    }

    private void setLeftBorder(LineType2 lineType, LineWidth lineWidth, String color){
        borderFill.createLeftBorder();
        borderFill.leftBorder().type(lineType);
        borderFill.leftBorder().width(lineWidth);
        borderFill.leftBorder().color(color);
    }

    private void setRightBorder(LineType2 lineType, LineWidth lineWidth, String color){
        borderFill.createRightBorder();
        borderFill.rightBorder().type(lineType);
        borderFill.rightBorder().width(lineWidth);
        borderFill.rightBorder().color(color);
    }

    private void setTopBorder(LineType2 lineType, LineWidth lineWidth, String color){
        borderFill.createTopBorder();
        borderFill.topBorder().type(lineType);
        borderFill.topBorder().width(lineWidth);
        borderFill.topBorder().color(color);
    }

    private void setBottomBorder(LineType2 lineType, LineWidth lineWidth, String color){
        borderFill.createBottomBorder();
        borderFill.bottomBorder().type(lineType);
        borderFill.bottomBorder().width(lineWidth);
        borderFill.bottomBorder().color(color);
    }

    private void setSlash(){
        borderFill.createSlash();
        borderFill.slash().Crooked(false);
        borderFill.slash().type(SlashType.NONE);
        borderFill.slash().isCounter(false);
    }

    private void setBackSlash(){
        borderFill.createBackSlash();
        borderFill.backSlash().Crooked(false);
        borderFill.backSlash().type(SlashType.NONE);
        borderFill.backSlash().isCounter(false);
    }

    /*
    private void setFillBrush(String faceColor, String hatchColor){
        borderFill.createFillBrush();
        FillBrush fillBrush = borderFill.fillBrush();
        fillBrush.createWinBrush();

        String fc = normalizeHex6(faceColor);
        if (fc == null) fc = "FFFFFF";

        // BGR로
        String bgr = fc.substring(4,6) + fc.substring(2,4) + fc.substring(0,2);

        fillBrush.winBrush().faceColor(bgr);
        fillBrush.winBrush().hatchColor(bgr);   // 검정 고정 금지
        fillBrush.winBrush().alpha(0f);         // 우선 0으로
    }
    */

    public BorderFill render() {
        setId();

        // spec이 없으면 기존 셀 옵션 사용
        if (leftSpec == null && rightSpec == null && topSpec == null && bottomSpec == null) {
            if (cell != null && cell.isBorder()) {
                String c = normalizeHex6OrHash(cell.getBorderColor());
                setLeftBorder(LineType2.SOLID, LineWidth.MM_0_1, c);
                setRightBorder(LineType2.SOLID, LineWidth.MM_0_1, c);
                setTopBorder(LineType2.SOLID, LineWidth.MM_0_1, c);
                setBottomBorder(LineType2.SOLID, LineWidth.MM_0_1, c);
            } else {
                String c = normalizeHex6OrHash(cell != null ? cell.getBorderColor() : "#000000");
                setLeftBorder(LineType2.NONE, LineWidth.MM_0_12, c);
                setRightBorder(LineType2.NONE, LineWidth.MM_0_12, c);
                setTopBorder(LineType2.NONE, LineWidth.MM_0_12, c);
                setBottomBorder(LineType2.NONE, LineWidth.MM_0_12, c);
                setSlash();
                setBackSlash();
            }

            // 여기서 반드시 add (이거 빠지면 기존 호환이 씹힘)
            headerXMLFile.refList().borderFills().add(borderFill);
            return borderFill;
        }

        // spec 적용
        applySide("left", leftSpec);
        applySide("right", rightSpec);
        applySide("top", topSpec);
        applySide("bottom", bottomSpec);

        setSlash();
        setBackSlash();

        // borderFill 등록은 여기서만
        headerXMLFile.refList().borderFills().add(borderFill);
        return borderFill;
    }

    private void applySide(String side, Map<String, Object> spec) {
        // spec이 null이면 기본값(기존 cell borderColor + 0.1 solid)
        LineType2 type = parseLineType(spec);
        LineWidth width = parseLineWidthExact(spec);   // "정확히 일치" 아니면 예외
        String color = parseColor(spec);

        if ("left".equals(side)) setLeftBorder(type, width, color);
        else if ("right".equals(side)) setRightBorder(type, width, color);
        else if ("top".equals(side)) setTopBorder(type, width, color);
        else if ("bottom".equals(side)) setBottomBorder(type, width, color);
    }

    private LineType2 parseLineType(Map<String, Object> spec) {
        // default
        if (spec == null) return LineType2.SOLID;

        Object s = spec.get("style");
        if (s == null) return LineType2.SOLID;

        String v = String.valueOf(s).trim();
        if (v.isEmpty()) return LineType2.SOLID;

        // normalize: lower + separators
        String key = v.toLowerCase()
                .replace('-', '_')
                .replace(' ', '_');

        // common aliases
        if ("hidden".equals(key)) return LineType2.NONE;

        // map to LineType2
        switch (key) {
            case "none": return LineType2.NONE;
            case "solid": return LineType2.SOLID;

            case "dot": return LineType2.DOT;
            case "dash": return LineType2.DASH;

            case "dash_dot":
            case "dashdot":
            case "dash-dot":
                return LineType2.DASH_DOT;

            case "dash_dot_dot":
            case "dashdotdot":
            case "dash-dot-dot":
                return LineType2.DASH_DOT_DOT;

            case "long_dash":
            case "longdash":
            case "long-dash":
                return LineType2.LONG_DASH;

            case "circle":
            case "circledot":
            case "circle_dot":
            case "circle-dot":
                return LineType2.CIRCLE;

            case "double_slim":
            case "double":
            case "double-slim":
                return LineType2.DOUBLE_SLIM;

            case "slim_thick":
            case "thinbold":
            case "slim-thick":
                return LineType2.SLIM_THICK;

            case "thick_slim":
            case "boldthin":
            case "thick-slim":
                return LineType2.THICK_SLIM;

            case "slim_thick_slim":
            case "thinboldthin":
            case "slim-thick-slim":
                return LineType2.SLIM_THICK_SLIM;

            default:
                // 마지막 방어: enum에 정확히 있으면 그걸로, 아니면 SOLID
                LineType2 byEnum = LineType2.fromString(key);
                return (byEnum != null) ? byEnum : LineType2.SOLID;
        }
    }


    /**
     * 요구사항:
     * - "60 -> 0.6" 같은 변환 절대 없음
     * - LineWidth enum에 없는 값이면 "오류 안내" (예외로 터뜨림)
     *
     * 허용값(예): 0.1, 0.12, 0.15, 0.2, 0.25, 0.3, 0.4, 0.5, 0.6, 0.7, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0
     */
    private LineWidth parseLineWidthExact(Map<String, Object> spec) {
        if (spec == null) return LineWidth.MM_0_1;

        Object wObj = spec.get("width");
        if (wObj == null) return LineWidth.MM_0_1;

        double mm;
        if (wObj instanceof Number) {
            mm = ((Number) wObj).doubleValue();
        } else {
            try { mm = Double.parseDouble(String.valueOf(wObj).trim()); }
            catch (Exception e) {
                throw new IllegalArgumentException("border.width must be a number (mm). got=" + wObj);
            }
        }

        // "정확히" 일치하는 enum만 허용
        LineWidth lw = matchExactLineWidth(mm);
        if (lw == null) {
            throw new IllegalArgumentException(
                    "Unsupported border width(mm): " + mm +
                    ". Allowed: 0.1,0.12,0.15,0.2,0.25,0.3,0.4,0.5,0.6,0.7,1.0,1.5,2.0,3.0,4.0,5.0"
            );
        }
        return lw;
    }

    private LineWidth matchExactLineWidth(double mm) {
        // floating 안전하게 문자열 기준도 같이 커버
        String s = stripTrailingZeros(mm);

        if (s.equals("0.1")) return LineWidth.MM_0_1;
        if (s.equals("0.12")) return LineWidth.MM_0_12;
        if (s.equals("0.15")) return LineWidth.MM_0_15;
        if (s.equals("0.2")) return LineWidth.MM_0_2;
        if (s.equals("0.25")) return LineWidth.MM_0_25;
        if (s.equals("0.3")) return LineWidth.MM_0_3;
        if (s.equals("0.4")) return LineWidth.MM_0_4;
        if (s.equals("0.5")) return LineWidth.MM_0_5;
        if (s.equals("0.6")) return LineWidth.MM_0_6;
        if (s.equals("0.7")) return LineWidth.MM_0_7;
        if (s.equals("1") || s.equals("1.0")) return LineWidth.MM_1_0;
        if (s.equals("1.5")) return LineWidth.MM_1_5;
        if (s.equals("2") || s.equals("2.0")) return LineWidth.MM_2_0;
        if (s.equals("3") || s.equals("3.0")) return LineWidth.MM_3_0;
        if (s.equals("4") || s.equals("4.0")) return LineWidth.MM_4_0;
        if (s.equals("5") || s.equals("5.0")) return LineWidth.MM_5_0;

        return null;
    }

    private String stripTrailingZeros(double v) {
        String s = String.valueOf(v);
        if (s.contains("E") || s.contains("e")) return s; // 혹시 모를 케이스
        if (!s.contains(".")) return s;
        // 0.600000 -> 0.6
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private String parseColor(Map<String, Object> spec) {
        String raw;
        if (spec != null && spec.get("color") != null) raw = String.valueOf(spec.get("color")).trim();
        else if (cell != null && cell.getBorderColor() != null) raw = String.valueOf(cell.getBorderColor()).trim();
        else raw = "#000000";

        return normalizeHex6OrHash(raw);
    }


    // "#000000" / "000000" / "abc" -> "000000" / "AABBCC" (6자리, 대문자)
    // 잘못된 값이면 기본값 "000000"으로 방어
    private String normalizeHex6OrHash(String hex) {
        String s = (hex == null) ? "" : hex.trim();
        if (s.isEmpty()) return "#000000";
        if (s.startsWith("#")) s = s.substring(1).trim();

        if (s.length() == 3) {
            s = "" + s.charAt(0)+s.charAt(0) + s.charAt(1)+s.charAt(1) + s.charAt(2)+s.charAt(2);
        }
        if (s.length() != 6) return "#000000";

        for (int i = 0; i < 6; i++) {
            char ch = s.charAt(i);
            boolean ok = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (!ok) return "#000000";
        }
        return "#" + s.toUpperCase();
    }

    private String normalizeHex6(String hex) {
        if (hex == null) return "000000";
        String s = hex.trim();
        if (s.isEmpty()) return "000000";
        if (s.startsWith("#")) s = s.substring(1).trim();

        if (s.length() == 3) {
            s = "" + s.charAt(0)+s.charAt(0) + s.charAt(1)+s.charAt(1) + s.charAt(2)+s.charAt(2);
        }
        if (s.length() != 6) return "000000";

        for (int i = 0; i < 6; i++) {
            char ch = s.charAt(i);
            boolean ok = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
            if (!ok) return "000000";
        }
        return s.toUpperCase();
    }
}
