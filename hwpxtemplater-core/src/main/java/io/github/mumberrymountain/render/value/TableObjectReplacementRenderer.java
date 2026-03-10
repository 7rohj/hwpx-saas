package io.github.mumberrymountain.render.value;

import io.github.mumberrymountain.linkedobj.LinkedRunItem;
import io.github.mumberrymountain.render.HWPXRenderer;
import io.github.mumberrymountain.render.TableRenderer;
import io.github.mumberrymountain.render.placeholder.PlaceHolder;
import io.github.mumberrymountain.render.placeholder.PlaceHolderRangeStack;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Run;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;

public class TableObjectReplacementRenderer implements ValueReplacementRenderer {

    private io.github.mumberrymountain.model.table.Table value;
    private final LinkedRunItem linkedRunItem;
    private final PlaceHolder placeHolder;
    private final HWPXRenderer rootRenderer;
    private final PlaceHolderRangeStack rangeStack;

    public TableObjectReplacementRenderer(
            io.github.mumberrymountain.model.table.Table value,
            LinkedRunItem linkedRunItem,
            PlaceHolder placeHolder,
            HWPXRenderer rootRenderer,
            PlaceHolderRangeStack rangeStack
    ) {
        this.value = value;
        this.linkedRunItem = linkedRunItem;
        this.placeHolder = placeHolder;
        this.rootRenderer = rootRenderer;
        this.rangeStack = rangeStack;
    }

    @Override
    public void render() {
        if (value == null) return;
        renderReplacement();
    }

    @Override
    public void executeNullValueInterceptor() {
        // Table은 문자열 기반 interceptor 대상 아님
    }

    @Override
    public void executeValueInterceptor() {
        // Table은 문자열 기반 interceptor 대상 아님
    }

    @Override
    public void executeTrim() {
        // Table은 trim 대상 아님
    }

    @Override
    public void renderReplacement() {
        int runItemIndex = linkedRunItem.parent().data().getRunItemIndex(linkedRunItem.data());

        // placeholder 텍스트 제거
        linkedRunItem.parent().data().removeRunItem(placeHolder.t());

        // hwpx table 생성
        Table table = new TableRenderer(rootRenderer, value).render();

        // 기존 placeholder 위치에 table 삽입
        Run pRun = linkedRunItem.parent().data();
        pRun.insertRunItem(table, runItemIndex);

        if (rangeStack != null && rangeStack.current() != null) {
            rangeStack.add(linkedRunItem.parent(), placeHolder);
        }
    }
}