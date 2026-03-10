package io.github.mumberrymountain.render;

import io.github.mumberrymountain.linkedobj.LinkedRunItem;
import io.github.mumberrymountain.render.placeholder.PlaceHolder;

public interface SinglePlaceHolderRenderer {
    public void renderReplacement(LinkedRunItem linkedRunItem, PlaceHolder placeHolder, Object value);
}
