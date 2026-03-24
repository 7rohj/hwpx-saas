package com.khwajeong.hwpx_saas.compose.model;

import java.util.List;

public class SourceAsset {

    private String id;
    private SourceAssetType type;

    private String text;
    private String caption;

    private String imagePath;

    private List<List<String>> tableData;

    public String getId() {
        return id;
    }

    public SourceAsset setId(String id) {
        this.id = id;
        return this;
    }

    public SourceAssetType getType() {
        return type;
    }

    public SourceAsset setType(SourceAssetType type) {
        this.type = type;
        return this;
    }

    public String getText() {
        return text;
    }

    public SourceAsset setText(String text) {
        this.text = text;
        return this;
    }

    public String getCaption() {
        return caption;
    }

    public SourceAsset setCaption(String caption) {
        this.caption = caption;
        return this;
    }

    public String getImagePath() {
        return imagePath;
    }

    public SourceAsset setImagePath(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }

    public List<List<String>> getTableData() {
        return tableData;
    }

    public SourceAsset setTableData(List<List<String>> tableData) {
        this.tableData = tableData;
        return this;
    }
}