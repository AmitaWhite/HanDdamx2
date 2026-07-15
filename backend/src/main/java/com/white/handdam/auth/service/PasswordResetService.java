package com.white.handdam.auth.service;

import com.white.handdam.auth.entity.EmailVerification;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.auth.util.EmailNormalizer;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.OAuthProvider;
import com.white.handdam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

    // KSY-012
    public void requestPasswordReset(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        memberRepository.findByEmail(normalizedEmail).ifPresent(member -> {
            // OAuth 전용 계정은 비밀번호 재설정 메일을 보내지 X
            if (member.getOauthProvider() != OAuthProvider.NONE) {
                return;
            } // 항상 동일한 응답 위해 예외 던지지 X

            emailVerificationService.issueAndSend(
                    member.getId(),
                    normalizedEmail,
                    member.getNickname(),
                    VerificationPurpose.PASSWORD_RESET
            );
        });
        // 있으면 메일 보냄 / 없으면 아무것도 안 함 - 클라이언트에게는 둘 다 성공 응답
        // Email Enumeration Attack 방지 위함
    }

    // KSY-0013
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        EmailVerification verification = emailVerificationService.confirm(
                rawToken,
                VerificationPurpose.PASSWORD_RESET
        );

        Member member = memberRepository.findById(verification.getMemberId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.MEMBER_NOT_FOUND));

        member.changePassword(passwordEncoder.encode(newPassword));

        // TODO: RefreshToken 도입 후 invalidateAll()
    }

}
