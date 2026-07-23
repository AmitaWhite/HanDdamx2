package com.white.handdam.creator.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * 크리에이터 전환 신청 엔티티.
 *
 */
@Entity
@Table(name = "creator_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreatorApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청자 회원 ID (FK → member.id) */
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 심사 상태: PENDING → APPROVED or REJECTED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CreatorApplicationStatus status;

    /** 심사한 관리자 회원 ID (FK → member.id, 심사 전 null) */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "introduction", length = 1000)
    private String introduction;

    @Column(name = "representative_image_url", length = 500)
    private String representativeImageUrl;

    /** 거절 사유 (거절 시에만 기록) */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 신청일 (INSERT 시 자동 설정) */
    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    /** 심사 완료일 (승인/거절 시 기록) */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist
    protected void onPersist() {
        this.appliedAt = Instant.now();
        this.status = CreatorApplicationStatus.PENDING;
    }

    public static CreatorApplication create(Long memberId, String introduction, String representativeImageUrl) {
        CreatorApplication application = new CreatorApplication();
        application.memberId = memberId;
        application.introduction = introduction;
        application.representativeImageUrl = representativeImageUrl;
        return application;
    }

    /**
     * 신청 승인 처리
     */
    public void approve(Long adminId) {
        this.status = CreatorApplicationStatus.APPROVED;
        this.reviewedBy = adminId;
        this.reviewedAt = Instant.now();
    }

    /** 신청 거절 처리 */
    public void reject(Long adminId, String rejectReason) {
        this.status = CreatorApplicationStatus.REJECTED;
        this.reviewedBy = adminId;
        this.rejectReason = rejectReason;
        this.reviewedAt = Instant.now();
    }
}
