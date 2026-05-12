package com.khwajeong.hwpx_saas.feedback.config;

import com.khwajeong.hwpx_saas.feedback.entity.FeedbackCommentEntity;
import com.khwajeong.hwpx_saas.feedback.repository.FeedbackCommentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class FeedbackCommentSeedData implements CommandLineRunner {

    private final FeedbackCommentRepository feedbackCommentRepository;

    public FeedbackCommentSeedData(FeedbackCommentRepository feedbackCommentRepository) {
        this.feedbackCommentRepository = feedbackCommentRepository;
    }

    @Override
    public void run(String... args) {
        if (feedbackCommentRepository.count() > 0) {
            return;
        }

        createComment(
                "wlgns908@example.com",
                "ㅇㅇ",
                "제가 필요했던 프로그램이에요... 보고서 수정할 때 표 하나하나 복붙하던 거 생각하면 진짜 눈물 납니다 ㅠㅠ 정식 출시 가자구",
                "2026-05-12T22:14:26+09:00"
        );

        createComment(
                "vkfksgksmf@example.com",
                "닉네임",
                "템플릿파일이랑 원본문서를 한 화면에서 비교할 수 있는 기능이 있으면 더 좋을 것 같네요. 그리고 표 안의 표도 잘 인식되면 실무에서 엄청 유용할 듯해요",
                "2026-05-12T22:37:02+09:00"
        );

        createComment(
                "rkdgksskawk@example.com",
                "말하는감자",
                "한글 문서 스타일 수정할 때 매번 Alt+C Alt+V 반복했는데 이런 서비스 처음 봐요 ㅋㅋㅋ UI도 깔끔해서 기대됩니다. 화이팅!",
                "2026-05-12T23:02:57+09:00"
        );
    }

    private void createComment(
            String userEmail,
            String maskedName,
            String content,
            String createdAt
    ) {
        FeedbackCommentEntity entity = new FeedbackCommentEntity();
        entity.setUserEmail(userEmail);
        entity.setMaskedName(maskedName);
        entity.setContent(content);
        entity.setCreatedAt(OffsetDateTime.parse(createdAt));

        feedbackCommentRepository.save(entity);
    }
}