package com.white.handdam.auth.entity;

import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.global.entity.BaseCreatedAtEntity;
import com.white.handdam.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "email_verification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id")
    private Long memberId; // 가입 전 인증 시 NULL

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationPurpose purpose;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder(access = AccessLevel.PRIVATE)
    public EmailVerification(Long memberId, String email, VerificationPurpose purpose,
                             String tokenHash, Instant expiresAt) {
        this.memberId = memberId;
        this.email = email;
        this.purpose = purpose;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification create(Long memberId, String email, VerificationPurpose purpose,
                                           String tokenHash, Instant expiresAt) {
        return EmailVerification.builder()
                .memberId(memberId)
                .email(email)
                .purpose(purpose)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
    }

    // 토큰 사용 가능한지 확인
    public void checkValid() {
        if (this.status == VerificationStatus.EXPIRED) {
            throw new CustomException(AuthErrorCode.EXPIRED_VERIFICATION_TOKEN);
        }
        if (this.status == VerificationStatus.VERIFIED) {
            throw new CustomException(AuthErrorCode.ALREADY_PROCESSED_VERIFICATION);
        }
        if (isExpired()) {
            throw new CustomException(AuthErrorCode.EXPIRED_VERIFICATION_TOKEN);
        }
    }

    // 검증 후 토큰 사용 처리
    public void verify() {
        checkValid();
        this.status = VerificationStatus.VERIFIED;
        this.usedAt = Instant.now();
    }

    public void expire() {
        this.status = VerificationStatus.EXPIRED;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

}
