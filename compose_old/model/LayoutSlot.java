package com.khwajeong.hwpx_saas.compose.model;

public class LayoutSlot {

    private String id;
    private LayoutSlotType type;
    private String placeholderKey;
    private String sampleText;

    public LayoutSlot() {
    }

    public LayoutSlot(String id, LayoutSlotType type, String placeholderKey, String sampleText) {
        this.id = id;
        this.type = type;
        this.placeholderKey = placeholderKey;
        this.sampleText = sampleText;
    }

    public String getId() {
        return id;
    }

    public LayoutSlot setId(String id) {
        this.id = id;
        return this;
    }

    public LayoutSlotType getType() {
        return type;
    }

    public LayoutSlot setType(LayoutSlotType type) {
        this.type = type;
        return this;
    }

    public String getPlaceholderKey() {
        return placeholderKey;
    }

    public LayoutSlot setPlaceholderKey(String placeholderKey) {
        this.placeholderKey = placeholderKey;
        return this;
    }

    public String getSampleText() {
        return sampleText;
    }

    public LayoutSlot setSampleText(String sampleText) {
        this.sampleText = sampleText;
        return this;
    }
}