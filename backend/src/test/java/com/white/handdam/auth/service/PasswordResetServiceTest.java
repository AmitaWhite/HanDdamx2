package com.white.handdam.auth.service;

import com.white.handdam.auth.entity.EmailVerification;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    // KSY-012
    @Test
    @DisplayName("존재하는 회원이면 비밀번호 재설정 메일 발송")
    void requestPasswordResetSuccess() {
        Member member = Member.createLocalMember("test@handdam.com", "encodedPassword", "테스트닉네임");
        given(memberRepository.findByEmail("test@handdam.com")).willReturn(Optional.of(member));

        passwordResetService.requestPasswordReset("test@handdam.com");

        then(emailVerificationService).should()
                .issueAndSend(member.getId(), "test@handdam.com", member.getNickname(), VerificationPurpose.PASSWORD_RESET);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 아무 것도 하지 않음 (이메일 존재 여부 노출 방지)")
    void requestPasswordResetWhenMemberNotFound() {
        given(memberRepository.findByEmail("nobody@handdam.com")).willReturn(Optional.empty());

        passwordResetService.requestPasswordReset("nobody@handdam.com");

        then(emailVerificationService).should(never()).issueAndSend(any(), any(), any(), any());
    }

    // KSY-013
    @Test
    @DisplayName("유효한 토큰이면 새 비밀번호로 변경")
    void resetPasswordSuccess() {
        Long memberId = 1L;
        EmailVerification ev = EmailVerification.create(
                memberId,
                "test@handdam.com",
                VerificationPurpose.PASSWORD_RESET,
                "tokenHash",
                Instant.now().plus(Duration.ofMinutes(10)));
        Member member = Member.createLocalMember("test@handdam.com", "oldEncodedPassword", "테스트닉네임");

        given(emailVerificationService.confirm("raw-token", VerificationPurpose.PASSWORD_RESET)).willReturn(ev);
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(passwordEncoder.encode("newPassword1!")).willReturn("newEncodedPassword");

        passwordResetService.resetPassword("raw-token", "newPassword1!");

        assertThat(member.getPassword()).isEqualTo("newEncodedPassword");
    }

    @Test
    @DisplayName("토큰 검증에 실패하면 예외 처리, 회원 조회 X")
    void resetPasswordFailWhenTokenInvalid() {
        given(emailVerificationService.confirm("raw-token", VerificationPurpose.PASSWORD_RESET))
                .willThrow(new CustomException(AuthErrorCode.EXPIRED_VERIFICATION_TOKEN));

        assertThatThrownBy(() -> passwordResetService.resetPassword("raw-token", "newPassword1!"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.EXPIRED_VERIFICATION_TOKEN);

        then(memberRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("토큰에 연결된 회원이 없으면 예외 처리")
    void resetPasswordFailWhenMemberNotFound() {
        Long memberId = 1L;
        EmailVerification ev = EmailVerification.create(
                memberId,
                "test@handdam.com",
                VerificationPurpose.PASSWORD_RESET,
                "tokenHash",
                Instant.now().plus(Duration.ofMinutes(10)));

        given(emailVerificationService.confirm("raw-token", VerificationPurpose.PASSWORD_RESET)).willReturn(ev);
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("raw-token", "newPassword1!"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.MEMBER_NOT_FOUND);
    }

}