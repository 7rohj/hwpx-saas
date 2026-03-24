package com.khwajeong.hwpx_saas.compose.service;

import com.khwajeong.hwpx_saas.compose.model.SourceAsset;
import com.khwajeong.hwpx_saas.compose.model.SourceAssetType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class SourceContentExtractor {

    public List<SourceAsset> extract(MultipartFile sourceFile) {
        // TODO:
        // 실제 원본문서(HWP/HWPX)에서
        // 이미지, 캡션, 표, 텍스트 블록을 추출해야 함.
        // 지금은 1차 뼈대용 샘플 데이터.

        List<SourceAsset> assets = new ArrayList<>();

        assets.add(new SourceAsset()
                .setId("asset1")
                .setType(SourceAssetType.TEXT)
                .setText("2. 사회경제지표 분석"));

        assets.add(new SourceAsset()
                .setId("asset2")
                .setType(SourceAssetType.TEXT)
                .setText("2.1 사회경제지표 분석 기준"));

        assets.add(new SourceAsset()
                .setId("asset3")
                .setType(SourceAssetType.IMAGE)
                .setImagePath("sample/image1.png")
                .setCaption("A3-1-2.1-01"));

        assets.add(new SourceAsset()
                .setId("asset4")
                .setType(SourceAssetType.TEXT)
                .setText("사회경제지표 정리 및 분석의 기준"));

        assets.add(new SourceAsset()
                .setId("asset5")
                .setType(SourceAssetType.TEXT)
                .setText("사회경제지표 분석은 장래 교통수요예측을 위한 기초자료이다."));

        assets.add(new SourceAsset()
                .setId("asset6")
                .setType(SourceAssetType.TABLE)
                .setCaption("사회경제지표 현황")
                .setTableData(List.of(
                        List.of("인구수", "종사자수", "학생수"),
                        List.of("A3-1-22-01", "A3-1-22-02", "A3-1-22-03"),
                        List.of("연평균 1.78% 증가", "연평균 7.20% 증가", "연평균 0.06% 증가")
                )));

        return assets;
    }
}