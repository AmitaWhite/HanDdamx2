package com.white.handdam.auth.entity;

import com.white.handdam.global.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    public static EmailVerification create(Long memberId, String email, VerificationPurpose purpose,
                                           String tokenHash, Instant expiresAt) {
        EmailVerification ev = new EmailVerification();
        ev.memberId = memberId;
        ev.email = email;
        ev.purpose = purpose;
        ev.tokenHash = tokenHash;
        ev.expiresAt = expiresAt;
        return ev;
    }

}
