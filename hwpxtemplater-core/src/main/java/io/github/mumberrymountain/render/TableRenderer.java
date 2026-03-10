package io.github.mumberrymountain.render;

import io.github.mumberrymountain.util.HWPXUnitUtil;
import kr.dogfoot.hwpxlib.object.content.section_xml.enumtype.*;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.Table;
import kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.object.table.Tr;

public class TableRenderer {
    private final HWPXRenderer rootRenderer;
    private final Table renderingTable;

    private final io.github.mumberrymountain.model.table.Table table;

    public TableRenderer(HWPXRenderer rootRenderer, io.github.mumberrymountain.model.table.Table table) {
        this.rootRenderer = rootRenderer;
        this.table = table;
        this.renderingTable = new Table();
    }

    private void renderRow(int rowIdx){
        Tr tr = renderingTable.addNewTr();

        for (int colIdx = 0; colIdx < table.getColCount(); colIdx++) {
            Object cellObj = table.getCell(rowIdx, colIdx);
            if (isCoveredCell(cellObj)) continue;

            new TableCellRenderer(rootRenderer, tr.addNewTc(), rowIdx, colIdx, table).render();
        }
    }

    private boolean isCoveredCell(Object cellObj) {
        if (cellObj == null) return false;
        try {
            java.lang.reflect.Method m = cellObj.getClass().getMethod("isCovered");
            Object r = m.invoke(cellObj);
            return (r instanceof Boolean) && (Boolean) r;
        } catch (Exception e) {
            return false;
        }
    }

    private void setTable(){
        boolean repeatHeader = true;
        Object opt = table.getConfig("repeatHeader");
        if (opt instanceof Boolean) repeatHeader = (Boolean) opt;

        renderingTable.idAnd(RendererUtil.getRandomId())
            .rowCntAnd((short) table.getRowCount())
            .colCntAnd((short) table.getColCount())
            .numberingTypeAnd(NumberingType.TABLE)
            .textWrapAnd(TextWrapMethod.TOP_AND_BOTTOM)
            .textFlowAnd(TextFlowSide.BOTH_SIDES)
            .lockAnd(false)
            .dropcapstyleAnd(DropCapStyle.None)
            .pageBreakAnd(TablePageBreak.CELL)
            .repeatHeaderAnd(repeatHeader)
            .noAdjustAnd(false)
            .cellSpacingAnd(0)
            .zOrderAnd(-1)
            .borderFillIDRefAnd("3");
    }

    private void setOutMargin(){
        renderingTable.createOutMargin();
        renderingTable.outMargin().leftAnd(283L).rightAnd(283L).topAnd(283L).bottomAnd(283L);
    }

    private void setInMargin(){
        renderingTable.createInMargin();
        renderingTable.inMargin().leftAnd(510L).rightAnd(510L).topAnd(141L).bottomAnd(141L);
    }

    private void setPos() {
        renderingTable.createPos();
        renderingTable.pos()
            .treatAsCharAnd(false).affectLSpacingAnd(false).flowWithTextAnd(true).allowOverlapAnd(false).holdAnchorAndSOAnd(false)
            .vertRelToAnd(VertRelTo.PARA).horzRelToAnd(HorzRelTo.COLUMN).vertAlignAnd(VertAlign.TOP).horzAlignAnd(HorzAlign.LEFT)
            .vertOffsetAnd(0L).horzOffsetAnd(0L);
    }

    private void setSz() {
        renderingTable.createSZ();

        long totalWidth = 0;
        for (int i = 0; i < table.getColCount(); i++) {
            totalWidth += (long) HWPXUnitUtil.pxToHwpxUnit(table.getCol(i).getWidth());
        }
        if (totalWidth <= 0) totalWidth = 42522;

        renderingTable.sz()
            .widthAnd(totalWidth).widthRelToAnd(WidthRelTo.ABSOLUTE)
            .heightAnd(1284L).heightRelToAnd(HeightRelTo.ABSOLUTE)
            .protectAnd(false);
    }

    public Table render(){
        setTable();
        setOutMargin();
        setInMargin();
        setPos();
        setSz();

        for (int rowIdx = 0; rowIdx < table.getRowCount(); rowIdx++) renderRow(rowIdx);

        return renderingTable;
    }
}
