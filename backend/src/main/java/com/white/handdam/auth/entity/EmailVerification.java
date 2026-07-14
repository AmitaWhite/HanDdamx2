package com.white.handdam.auth.entity;

import com.white.handdam.global.entity.BaseCreatedAtEntity;
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

    public void verify() {
        if(this.status != VerificationStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 인증 토큰입니다.");
        }
        if(isExpired()) {
            throw new IllegalStateException("만료된 인증 토큰입니다.");
        }
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
