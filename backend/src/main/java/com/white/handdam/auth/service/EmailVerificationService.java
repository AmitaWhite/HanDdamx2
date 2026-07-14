package com.white.handdam.auth.service;

import com.white.handdam.auth.entity.EmailVerification;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.event.VerificationEmailRequestedEvent;
import com.white.handdam.auth.repository.EmailVerificationRepository;
import com.white.handdam.auth.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofMinutes(10);

    @Transactional
    public void issueAndSend(Long memberId, String email, String nickname, VerificationPurpose purpose) {
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

}