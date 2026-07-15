package com.white.handdam.auth.service;

import com.white.handdam.auth.dto.LoginResult;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.auth.util.EmailNormalizer;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.security.jwt.JwtTokenProvider;
import com.white.handdam.global.security.jwt.RefreshTokenRepository;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginResult login(String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);

        Member member = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_CREDENTIALS)); // MEMBER_NOT_FOUND 사용 X - 인증된 사용자 API에서만 사용

        // OAuth 계정인 경우
        if (member.getPassword() == null) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        // 비밀번호 불일치
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new CustomException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        // 이메일 인증 X
        if (!member.isEmailVerified()) {
            throw new CustomException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId(), member.getRole());

        refreshTokenRepository.save(member.getId(), refreshToken); // 회원당 refresh token 1개 저장 - 신규 로그인 시 기존 refresh token 무효화

        return new LoginResult(accessToken, refreshToken, member.getId(), member.getNickname(), member.getRole());
    }

    public void logout(Long memberId) {
        refreshTokenRepository.deleteByMemberId(memberId); // redis에서 삭제
    }

}
