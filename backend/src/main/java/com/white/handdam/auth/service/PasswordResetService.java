package com.white.handdam.auth.service;

import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.util.EmailNormalizer;
import com.white.handdam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final MemberRepository memberRepository;
    private final EmailVerificationService emailVerificationService;

    // KSY-012
    public void requestPasswordReset(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        memberRepository.findByEmail(normalizedEmail).ifPresent(member -> {
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

}
