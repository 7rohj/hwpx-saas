package io.github.mumberrymountain.render;

import io.github.mumberrymountain.model.table.Cell;
import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import io.github.mumberrymountain.Config;
import io.github.mumberrymountain.ConfigOption;
import io.github.mumberrymountain.model.Text;
import io.github.mumberrymountain.model.table.Align;
import io.github.mumberrymountain.render.placeholder.PlaceHolderRange;
import io.github.mumberrymountain.render.placeholder.Pos;
import io.github.mumberrymountain.util.Status;

import java.util.UUID;

import static io.github.mumberrymountain.render.placeholder.PlaceHolderType.LOOP;

public class RendererUtil {

    public static boolean isRangeVertical(PlaceHolderRange currentRange){
        return currentRange != null && currentRange.pairAlignment() == Pos.VERTICAL
                && currentRange.start().parent().parent() instanceof SectionXMLFile;
    }

    public static boolean isCurrentRangeProcessing(PlaceHolderRange currentRange){
        return currentRange != null && currentRange.status() == Status.PROCESSING;
    }

    public static boolean isCurrentRangeLoop(PlaceHolderRange currentRange){
        return currentRange != null && currentRange.type() == LOOP;
    }

    public static String getRandomId() {
        return UUID.randomUUID().toString();
    }

    public static Boolean isAutoTrim(Config config){
        return (Boolean) config.get(ConfigOption.AUTO_TRIM.getType());
    }

    public static String createCharStyleKey(Text text) {
        return String.join(";",
                    String.valueOf(text.getFontSize()),
                    String.valueOf(text.getFontColor()),
                    String.valueOf(text.getFontFamily()),
                    String.valueOf(text.getBackgroundColor()),
                    String.valueOf(text.isBold()),
                    String.valueOf(text.isItalic()),
                    String.valueOf(text.isUnderLine()),
                    String.valueOf(text.isStrikeOut()),
                    String.valueOf(text.isOutline()),
                    String.valueOf(text.isShadow()),
                    String.valueOf(text.isEmboss()),
                    String.valueOf(text.isEngrave())
                );
    }

    public static String createParaStyleKey(Align align) {
        return String.join(";",
                String.valueOf(align)
               );
    }

    public static String createBorderStyleKey(Cell cell) {
        return String.join(";",
                String.valueOf(cell.getBorderColor()),
                String.valueOf(cell.getBackgroundColor()),
                String.valueOf(cell.isBorder())
        );
    }

    public static boolean isFullWidthPlaceHolder(char c) {
        return c == '？' || c == '＃' || c == '／' || c == '＄' || c == '＠';
    }

    public static char normalizeFullWidthPlaceHolder(char c) {
        switch (c) {
            case '？': return '?';
            case '＃': return '#';
            case '／': return '/';
            case '＄': return '$';
            case '＠': return '@';
            default: return c;
        }
    }
}
