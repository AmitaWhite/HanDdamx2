package com.white.handdam.auth.service;

import com.white.handdam.auth.dto.request.SignupRequest;
import com.white.handdam.auth.dto.response.SignupResponse;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.auth.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;

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
        if (memberRepository.existsByNickname(request.nickname())) {
            throw new CustomException(AuthErrorCode.DUPLICATE_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.createLocalMember(
                normalizedEmail,
                encodedPassword,
                request.nickname());
        memberRepository.save(member);

        emailVerificationService.issueAndSend(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                VerificationPurpose.SIGNUP);

        return SignupResponse.from(member);
    }

    // KSY-004
    @Transactional
    public void sendVerificationEmail(String email) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        memberRepository.findByEmail(normalizedEmail).ifPresent(member -> {
            // 이미 인증된 회원이면 무시
            if (member.isEmailVerified()) return;

            emailVerificationService.issueAndSend(
                    member.getId(),
                    member.getEmail(),
                    member.getNickname(),
                    VerificationPurpose.SIGNUP
            );

        });
    }

}
