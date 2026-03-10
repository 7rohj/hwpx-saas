package io.github.mumberrymountain.render.value;

public interface ValueReplacementRenderer {
    void render();
    void executeNullValueInterceptor();
    void executeValueInterceptor();
    void executeTrim();
    void renderReplacement();
}
