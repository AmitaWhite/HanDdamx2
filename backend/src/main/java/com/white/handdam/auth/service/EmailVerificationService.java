package com.white.handdam.auth.service;

import com.white.handdam.auth.entity.EmailVerification;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.entity.VerificationStatus;
import com.white.handdam.auth.event.VerificationEmailRequestedEvent;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.auth.repository.EmailVerificationRepository;
import com.white.handdam.auth.util.TokenGenerator;
import com.white.handdam.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RateLimitService rateLimitService;

    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofMinutes(10);

    @Transactional
    public void issueAndSend(Long memberId, String email, String nickname, VerificationPurpose purpose) {
        rateLimitService.checkCooldown(email, purpose);
        rateLimitService.increaseDailyCount(email, purpose);

        // 이전 PENDING 정리 후 토큰 발급
        expirePendingTokens(email, purpose);

        String rawToken = TokenGenerator.generateOpaqueToken();
        String tokenHash = TokenGenerator.hash(rawToken);

        // 인증 요청 생성
        EmailVerification verification = EmailVerification.create(
                memberId,
                email,
                purpose,
                tokenHash, // DB에 hash 저장
                Instant.now().plus(VERIFICATION_TOKEN_TTL));
        emailVerificationRepository.save(verification);

        eventPublisher.publishEvent(
                new VerificationEmailRequestedEvent(email, nickname, rawToken, purpose));
    }

    // KSY-005 & KSY-013
    @Transactional
    public EmailVerification confirm(String rawToken, VerificationPurpose expectedPurpose) {
        String tokenHash = TokenGenerator.hash(rawToken);

        EmailVerification ev = emailVerificationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_VERIFICATION_TOKEN));

        if (ev.getPurpose() != expectedPurpose) {
            throw new CustomException(AuthErrorCode.INVALID_VERIFICATION_TOKEN);
        }

        ev.verify(); // 상태 변경
        return ev;
    }

    // KSY-006
    private void expirePendingTokens(String email, VerificationPurpose purpose) {
        emailVerificationRepository.findAllByEmailAndPurposeAndStatus(
                email, purpose, VerificationStatus.PENDING)
                .forEach(EmailVerification::expire);
    }

}