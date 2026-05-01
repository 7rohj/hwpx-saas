package com.khwajeong.hwpx_saas.service;

import com.khwajeong.hwpx_saas.db.EmailVerificationEntity;
import com.khwajeong.hwpx_saas.db.EmailVerificationRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationRepository emailVerificationRepository,
            JavaMailSender mailSender
    ) {
        this.emailVerificationRepository = emailVerificationRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void sendCode(String email, String clientIp) {
        String normalizedEmail = normalize(email);

        long ipCount = emailVerificationRepository.countByClientIpAndCreatedAtAfter(
                clientIp,
                OffsetDateTime.now().minusDays(1)
        );

        if (ipCount >= 5) {
            throw new IllegalStateException("이 기기에서는 하루 5회까지만 인증번호를 요청할 수 있습니다. 소셜로그인을 이용해주세요.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));

        EmailVerificationEntity entity = new EmailVerificationEntity();
        entity.setEmail(normalizedEmail);
        entity.setCode(code);
        entity.setClientIp(clientIp); // 🔥 추가
        entity.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
        entity.setVerified(false);

        emailVerificationRepository.save(entity);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(normalizedEmail);
        message.setSubject("[AutoHWPX] 이메일 인증번호");
        message.setText(
                "AutoHWPX 이메일 인증번호입니다.\n\n" +
                "인증번호: " + code + "\n\n" +
                "이 인증번호는 5분 동안 유효합니다."
        );

        mailSender.send(message);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        String normalizedEmail = normalize(email);

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("인증번호를 입력해주세요.");
        }

        EmailVerificationEntity entity = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("인증번호를 먼저 발송해주세요."));

        if (entity.isVerified()) {
            return;
        }

        if (OffsetDateTime.now().isAfter(entity.getExpiresAt())) {
            throw new IllegalStateException("인증번호가 만료되었습니다. 다시 발송해주세요.");
        }

        if (!entity.getCode().equals(code.trim())) {
            throw new IllegalArgumentException("인증번호가 올바르지 않습니다.");
        }

        entity.setVerified(true);
        entity.setVerifiedAt(OffsetDateTime.now());
        emailVerificationRepository.save(entity);
    }

    public boolean isVerified(String email) {
        String normalizedEmail = normalize(email);

        return emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(normalizedEmail)
                .filter(EmailVerificationEntity::isVerified)
                .filter(entity -> entity.getVerifiedAt() != null)
                .filter(entity -> entity.getVerifiedAt().isAfter(OffsetDateTime.now().minusMinutes(30)))
                .isPresent();
    }

    private String normalize(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}