package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.LayoutSlot;
import com.khwajeong.hwpx_saas.compose.model.LayoutSlotType;
import com.khwajeong.hwpx_saas.compose.model.SourceAsset;
import com.khwajeong.hwpx_saas.compose.model.SourceAssetType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComposeLayoutRenderer {

    public Map<String, Object> buildRenderData(
            List<LayoutSlot> slots,
            List<SourceAsset> assets
    ) {
        Map<String, Object> data = new HashMap<>();

        SourceAsset firstTitle = findFirstText(assets, 0);
        SourceAsset secondTitle = findFirstText(assets, 1);
        SourceAsset thirdTitle = findFirstText(assets, 2);

        SourceAsset firstImage = findFirstAsset(assets, SourceAssetType.IMAGE, 0);
        SourceAsset firstTable = findFirstAsset(assets, SourceAssetType.TABLE, 0);

        for (LayoutSlot slot : slots) {
            LayoutSlotType type = slot.getType();
            String key = slot.getPlaceholderKey();

            switch (type) {
                case CHAPTER_TITLE -> {
                    if (firstTitle != null) data.put(key, firstTitle.getText());
                }
                case SECTION_TITLE -> {
                    if (secondTitle != null) data.put(key, secondTitle.getText());
                }
                case SUBSECTION_TITLE -> {
                    if (thirdTitle != null) data.put(key, thirdTitle.getText());
                }
                case IMAGE -> {
                    if (firstImage != null) data.put(key, firstImage.getImagePath());
                }
                case IMAGE_CAPTION -> {
                    if (firstImage != null) data.put(key, firstImage.getCaption());
                }
                case TABLE -> {
                    if (firstTable != null) data.put(key, firstTable.getTableData());
                }
                case TABLE_CAPTION -> {
                    if (firstTable != null) data.put(key, firstTable.getCaption());
                }
                case BODY -> {
                    SourceAsset body = findLongBodyText(assets);
                    if (body != null) data.put(key, body.getText());
                }
                default -> {
                }
            }
        }

        return data;
    }

    private SourceAsset findFirstText(List<SourceAsset> assets, int index) {
        return assets.stream()
                .filter(a -> a.getType() == SourceAssetType.TEXT && a.getText() != null)
                .skip(index)
                .findFirst()
                .orElse(null);
    }

    private SourceAsset findFirstAsset(List<SourceAsset> assets, SourceAssetType type, int index) {
        return assets.stream()
                .filter(a -> a.getType() == type)
                .skip(index)
                .findFirst()
                .orElse(null);
    }

    private SourceAsset findLongBodyText(List<SourceAsset> assets) {
        return assets.stream()
                .filter(a -> a.getType() == SourceAssetType.TEXT && a.getText() != null)
                .filter(a -> a.getText().length() > 20)
                .findFirst()
                .orElse(null);
    }
}