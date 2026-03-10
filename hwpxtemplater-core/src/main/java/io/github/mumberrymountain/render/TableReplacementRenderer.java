package io.github.mumberrymountain.render;

import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;
import io.github.mumberrymountain.linkedobj.LinkedRunItem;
import io.github.mumberrymountain.render.placeholder.PlaceHolder;
import io.github.mumberrymountain.render.placeholder.PlaceHolderRangeStack;

import java.util.logging.Logger;

import static io.github.mumberrymountain.render.RendererUtil.isCurrentRangeLoop;
import static io.github.mumberrymountain.render.RendererUtil.isCurrentRangeProcessing;

public class TableReplacementRenderer <H> implements SinglePlaceHolderRenderer{
    private PlaceHolderRangeStack rangeStack;
    private final HWPXRenderer rootRenderer;
    private static final Logger logger = Logger.getLogger(TableReplacementRenderer.class.getName());

    public TableReplacementRenderer(PlaceHolderRangeStack rangeStack, HWPXRenderer rootRenderer) {
        this.rangeStack = rangeStack;
        this.rootRenderer = rootRenderer;
    }

    @Override
    public void renderReplacement(LinkedRunItem linkedRunItem, PlaceHolder placeHolder, Object value) {
        if(isCurrentRangeLoop(rangeStack.current()) && isCurrentRangeProcessing(rangeStack.current())) {
            rangeStack.add(linkedRunItem.parent(), placeHolder);
        } else {
            if (value == null || !(value instanceof io.github.mumberrymountain.model.table.Table)) {
                logger.warning(String.format("Value for table template field '%s' must be Table, not %s '%s'",
                        placeHolder.data(), value == null ? "null" : value.getClass().getName(),
                        value == null ? "null" : value.toString()));
                return;
            }

            int runItemIndex = linkedRunItem.parent().data().getRunItemIndex(linkedRunItem.data());
            linkedRunItem.parent().data().removeRunItem(placeHolder.t());

            Table table = new TableRenderer(rootRenderer, (io.github.mumberrymountain.model.table.Table) value).render();
            
            Run pRun = linkedRunItem.parent().data();
            pRun.insertRunItem(table, runItemIndex);

            if (isCurrentRangeProcessing(rangeStack.current())) rangeStack.add(linkedRunItem.parent(), placeHolder);
        }
    }
}
