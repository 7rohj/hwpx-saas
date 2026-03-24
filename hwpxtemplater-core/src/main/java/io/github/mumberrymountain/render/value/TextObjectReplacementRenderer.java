package io.github.mumberrymountain.render.value;

import io.github.mumberrymountain.interceptor.InterceptorType;
import io.github.mumberrymountain.interceptor.NullValueInterceptor;
import io.github.mumberrymountain.interceptor.ValueInterceptor;
import io.github.mumberrymountain.interceptor.ValueStylingInterceptor;
import io.github.mumberrymountain.linkedobj.LinkedRunItem;
import io.github.mumberrymountain.model.Text;
import io.github.mumberrymountain.render.HWPXRenderer;
import io.github.mumberrymountain.render.RendererUtil;
import io.github.mumberrymountain.render.placeholder.PlaceHolder;
import io.github.mumberrymountain.render.placeholder.PlaceHolderRangeStack;

public class TextObjectReplacementRenderer implements ValueReplacementRenderer {
    private Text value;
    private final LinkedRunItem linkedRunItem;
    private final PlaceHolder placeHolder;
    private final HWPXRenderer rootRenderer;
    private final PlaceHolderRangeStack rangeStack;

    public TextObjectReplacementRenderer(Text value, LinkedRunItem linkedRunItem, PlaceHolder placeHolder, HWPXRenderer rootRenderer, PlaceHolderRangeStack rangeStack) {
        this.value = value;
        this.linkedRunItem = linkedRunItem;
        this.placeHolder = placeHolder;
        this.rootRenderer = rootRenderer;
        this.rangeStack = rangeStack;
    }

    @Override
    public void render() {
        if(value.getValue() == null) executeNullValueInterceptor();
        executeValueInterceptor();
        executeValueStylingInterceptor();
        if (value.getValue() == null) return;
        executeTrim();
        renderReplacement();
    }

    @Override
    public void executeNullValueInterceptor() {
        NullValueInterceptor nullValueInterceptor = (NullValueInterceptor) rootRenderer.interceptorHandler().get(InterceptorType.NullValueInterceptor);
        if (nullValueInterceptor != null) value.setValue(nullValueInterceptor.intercept(value.getValue(), placeHolder.data()));
    }

    @Override
    public void executeValueInterceptor() {
        ValueInterceptor valueInterceptor = (ValueInterceptor) rootRenderer.interceptorHandler().get(InterceptorType.ValueInterceptor);
        if (valueInterceptor != null) value.setValue(valueInterceptor.intercept(value.getValue(), placeHolder.data()));
    }

    public void executeValueStylingInterceptor() {
        ValueStylingInterceptor valueStylingInterceptor = (ValueStylingInterceptor) rootRenderer.interceptorHandler().get(InterceptorType.ValueStylingInterceptor);
        if (valueStylingInterceptor != null) {
            Text result = valueStylingInterceptor.intercept(value, placeHolder.data());
            if (result != null) value = result;
        }
    }

    @Override
    public void executeTrim() {
        if (RendererUtil.isAutoTrim(rootRenderer.config())) value.setValue(value.getValue().trim());
    }

    @Override
    public void renderReplacement() {
        System.out.println("DESC_RAW=[" + value.getValue().replace("\n","\\n").replace("\r","\\r") + "]");

        placeHolder.t().clear();

        String raw = value.getValue();
        if (raw == null) return;

        String[] parts = raw.split("\\r\\n|\\r|\\n", -1);
        for (int i = 0; i < parts.length; i++) {
            placeHolder.t().addText(parts[i]);
            if (i < parts.length - 1) {
                placeHolder.t().addNewLineBreak();
            }
        }

        kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para para =
            (kr.dogfoot.hwpxlib.object.content.section_xml.paragraph.Para)
                linkedRunItem.parent().parent().data();

        String preferredCharBaseId = value.getBaseCharPrId();
            if (preferredCharBaseId != null && !preferredCharBaseId.isBlank()) {
                linkedRunItem.parent().data().charPrIDRef(preferredCharBaseId);
            }

            linkedRunItem.parent().data().charPrIDRef(
                rootRenderer.styleRenderer().renderCharStyleAndReturnCharPrId(value)
            );

            io.github.mumberrymountain.model.table.Align alignEnum =
                    io.github.mumberrymountain.model.table.Align.Left;

            String al = value.getAlign();
            if (al != null) al = al.trim().toLowerCase();

            if ("center".equals(al)) {
                alignEnum = io.github.mumberrymountain.model.table.Align.Center;
            } else if ("right".equals(al)) {
                alignEnum = io.github.mumberrymountain.model.table.Align.Right;
            }

            Integer lineSpacingPercent = value.getLineSpacingPercent();
            Integer spaceBeforePt = value.getSpaceBefore();
            Integer spaceAfterPt  = value.getSpaceAfter();

            boolean needParaStyle =
                    alignEnum != io.github.mumberrymountain.model.table.Align.Left
                    || lineSpacingPercent != null
                    || spaceBeforePt != null
                    || spaceAfterPt != null
                    || (value.getBaseParaPrId() != null && !value.getBaseParaPrId().isBlank());

            if (needParaStyle) {
                String baseId = value.getBaseParaPrId();

                if (baseId == null || baseId.isBlank()) {
                    baseId = para.paraPrIDRef();
                }

                para.paraPrIDRef(
                        rootRenderer.styleRenderer().renderParaStyleFromBaseAndReturnParaPrId(
                                baseId,
                                alignEnum,
                                lineSpacingPercent,
                                spaceBeforePt,
                                spaceAfterPt
                        )
                );
            }

        para.removeLineSegArray();

        if (RendererUtil.isCurrentRangeProcessing(rangeStack.current())) {
            rangeStack.add(linkedRunItem.parent(), placeHolder);
        }
        
    }
}

