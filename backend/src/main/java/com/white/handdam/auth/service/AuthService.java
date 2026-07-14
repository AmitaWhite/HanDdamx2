package com.white.handdam.auth.service;

import com.white.handdam.auth.EmailVerificationRepository;
import com.white.handdam.auth.dto.request.SignupRequest;
import com.white.handdam.auth.dto.response.SignupResponse;
import com.white.handdam.auth.entity.EmailVerification;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.event.SignupCompletedEvent;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.auth.util.TokenGenerator;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.auth.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    private static final Duration SIGNUP_VERIFICATION_TOKEN_TTL = Duration.ofMinutes(10);

    // KSY-001
    public boolean isEmailAvailable(String email) {
        return !memberRepository.existsByEmail(EmailNormalizer.normalize(email));
    }

    // KSY-002
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

    // KSY-003
    @Transactional
    public SignupResponse signup(SignupRequest request) {

        String normalizedEmail = EmailNormalizer.normalize(request.email());

        // 이메일 중복 확인
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 닉네임 중복 확인
        if (memberRepository.existsByNickname(normalizedEmail)) {
            throw new CustomException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.createLocalMember(
                normalizedEmail,
                encodedPassword,
                request.nickname());
        memberRepository.save(member);

        String rawToken = issueVerificationToken(
                member.getId(),
                member.getEmail(),
                VerificationPurpose.SIGNUP);

        eventPublisher.publishEvent(
                new SignupCompletedEvent(
                        member.getId(),
                        member.getEmail(),
                        member.getNickname(),
                        rawToken));

        return SignupResponse.from(member);
    }

    private String issueVerificationToken(Long memberId, String email, VerificationPurpose purpose) {
        String rawToken = TokenGenerator.generateOpaqueToken();
        String tokenHash = TokenGenerator.hash(rawToken);

        EmailVerification verification =
                EmailVerification.create(memberId,
                                            email,
                                            purpose,
                                            tokenHash,
                                            Instant.now().plus(SIGNUP_VERIFICATION_TOKEN_TTL)
                );
        emailVerificationRepository.save(verification);
        return rawToken;
    }

}
