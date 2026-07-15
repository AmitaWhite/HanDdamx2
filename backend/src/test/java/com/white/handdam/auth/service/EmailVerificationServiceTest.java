package com.white.handdam.auth.service;

import com.white.handdam.auth.entity.EmailVerification;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.entity.VerificationStatus;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.auth.repository.EmailVerificationRepository;
import com.white.handdam.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    // KSY-005
    @Test
    @DisplayName("유효한 토큰이면 VERIFIED로 전환하고 반환")
    void confirmSuccess() {
        EmailVerification ev = EmailVerification.create(
                1L,
                "test@handdam.com",
                VerificationPurpose.SIGNUP,
                "tokenHash",
                Instant.now().plus(Duration.ofMinutes(10)));
        given(emailVerificationRepository.findByTokenHash(anyString())).willReturn(Optional.of(ev));

        EmailVerification result = emailVerificationService.confirm("raw-token", VerificationPurpose.SIGNUP);

        assertThat(result.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(result.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 토큰이면 예외를 던짐")
    void confirmFailWhenTokenNotFound() {
        given(emailVerificationRepository.findByTokenHash(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.confirm("raw-token", VerificationPurpose.SIGNUP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_VERIFICATION_TOKEN);
    }

    @Test
    @DisplayName("토큰의 목적이 기대한 목적과 다르면 예외를 던지고 상태를 변경하지 않음")
    void confirmFailWhenPurposeMismatch() {
        EmailVerification ev = EmailVerification.create(
                1L,
                "test@handdam.com",
                VerificationPurpose.PASSWORD_RESET,
                "tokenHash",
                Instant.now().plus(Duration.ofMinutes(10)));
        given(emailVerificationRepository.findByTokenHash(anyString())).willReturn(Optional.of(ev));

        assertThatThrownBy(() -> emailVerificationService.confirm("raw-token", VerificationPurpose.SIGNUP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_VERIFICATION_TOKEN);

        assertThat(ev.getStatus()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    @DisplayName("만료 시각이 지난 토큰이면 예외를 던짐")
    void confirmFailWhenExpiresAtPassed() {
        EmailVerification ev = EmailVerification.create(
                1L,
                "test@handdam.com",
                VerificationPurpose.SIGNUP,
                "tokenHash",
                Instant.now().minus(Duration.ofMinutes(1)));
        given(emailVerificationRepository.findByTokenHash(anyString())).willReturn(Optional.of(ev));

        assertThatThrownBy(() -> emailVerificationService.confirm("raw-token", VerificationPurpose.SIGNUP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.EXPIRED_VERIFICATION_TOKEN);
    }

    @Test
    @DisplayName("이미 EXPIRED 처리된 토큰이면 예외를 던짐")
    void confirmFailWhenStatusExpired() {
        EmailVerification ev = EmailVerification.create(
                1L,
                "test@handdam.com",
                VerificationPurpose.SIGNUP,
                "tokenHash",
                Instant.now().plus(Duration.ofMinutes(10)));
        ev.expire();
        given(emailVerificationRepository.findByTokenHash(anyString())).willReturn(Optional.of(ev));

        assertThatThrownBy(() -> emailVerificationService.confirm("raw-token", VerificationPurpose.SIGNUP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.EXPIRED_VERIFICATION_TOKEN);
    }

    @Test
    @DisplayName("이미 VERIFIED 처리된 토큰이면 예외를 던짐")
    void confirmFailWhenAlreadyVerified() {
        EmailVerification ev = EmailVerification.create(
                1L,
                "test@handdam.com",
                VerificationPurpose.SIGNUP,
                "tokenHash",
                Instant.now().plus(Duration.ofMinutes(10)));
        ev.verify();
        given(emailVerificationRepository.findByTokenHash(anyString())).willReturn(Optional.of(ev));

        assertThatThrownBy(() -> emailVerificationService.confirm("raw-token", VerificationPurpose.SIGNUP))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.ALREADY_PROCESSED_VERIFICATION);
    }

}