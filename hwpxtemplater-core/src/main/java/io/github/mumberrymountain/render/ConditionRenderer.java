package io.github.mumberrymountain.render;

import kr.dogfoot.hwpxlib.object.content.section_xml.SectionXMLFile;
import io.github.mumberrymountain.linkedobj.LinkedPara;
import io.github.mumberrymountain.linkedobj.LinkedRun;
import io.github.mumberrymountain.render.placeholder.PlaceHolder;
import io.github.mumberrymountain.render.placeholder.PlaceHolderRangeStack;
import io.github.mumberrymountain.util.Status;
import io.github.mumberrymountain.util.ParaUtil;

public class ConditionRenderer<H> implements PairedPlaceHolderRenderer{
    private final PlaceHolderRangeStack rangeStack;
    private final HWPXRenderer rootRenderer;

    public ConditionRenderer(PlaceHolderRangeStack rangeStack, HWPXRenderer rootRenderer) {
        this.rangeStack = rangeStack;
        this.rootRenderer = rootRenderer;
    }

    private void clearParaInCurrentStack(){
        SectionXMLFile section = (SectionXMLFile) rangeStack.current().start().parent().parent();

        int startParaIndex = section.getParaIndex(rangeStack.current().start().parent().data());
        int endParaIndex = section.getParaIndex(rangeStack.current().end().parent().data());
        int idx = endParaIndex;

        while (idx >= startParaIndex) {
            section.removePara(section.getPara(idx));
            idx--;
        }
    }

    private void clearConditionPara(LinkedPara para) {
        if (para.parent() instanceof SectionXMLFile) {
            SectionXMLFile section= (SectionXMLFile) para.parent();
            section.removePara(para.data());
        }
    }

    private void clearCondition(LinkedRun linkedRun) {
        linkedRun.parent().data().removeRun(linkedRun.data());

        if(!ParaUtil.containsT(linkedRun.parent().data())) {
            clearConditionPara(linkedRun.parent());
        }
    }

    private void flushRangeCondition(PlaceHolder placeHolder){
        Object value = rootRenderer.data().get(placeHolder.data());
        if (value instanceof Boolean && (Boolean) value == false) {
            if(RendererUtil.isRangeVertical(rangeStack.current())) clearParaInCurrentStack();
            else for (LinkedRun r : rangeStack.current().linkedRuns()) clearCondition(r);
        } else {
            clearCondition(rangeStack.current().start());
            clearCondition(rangeStack.current().end());
        }
    }

    @Override
    public void flushRange(PlaceHolder placeHolder) {
        rangeStack.current().setStatus(Status.FINISH);
        rangeStack.current().setPlaceHolderText(null);
        flushRangeCondition(placeHolder);
    }
}
