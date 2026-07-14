package com.white.handdam.creator.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 크리에이터 전용 프로필 엔티티.
 */
@Entity
@Table(name = "creator_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreatorProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 크리에이터 회원 ID (FK → member.id, UNIQUE) */
    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    /** 크리에이터 소개글 */
    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    /** 대표 이미지 URL */
    @Column(name = "representative_image_url", length = 500)
    private String representativeImageUrl;

    /** 대표 이미지 S3 객체 키 */
    @Column(name = "representative_image_storage_key", length = 500)
    private String representativeImageStorageKey;

    /** 커버 이미지 URL */
    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    /** 커버 이미지 S3 객체 키 */
    @Column(name = "cover_image_storage_key", length = 500)
    private String coverImageStorageKey;

    /** 유료 구독 혜택 설명 */
    @Column(name = "benefits_description", columnDefinition = "TEXT")
    private String benefitsDescription;

    /** 월 구독료 (원, 0 이상) */
    @Column(name = "subscription_price", nullable = false)
    private int subscriptionPrice;

    @Builder
    private CreatorProfile(Long memberId, String introduction,
                           String representativeImageUrl, String representativeImageStorageKey,
                           String coverImageUrl, String coverImageStorageKey,
                           String benefitsDescription, int subscriptionPrice) {
        this.memberId = memberId;
        this.introduction = introduction;
        this.representativeImageUrl = representativeImageUrl;
        this.representativeImageStorageKey = representativeImageStorageKey;
        this.coverImageUrl = coverImageUrl;
        this.coverImageStorageKey = coverImageStorageKey;
        this.benefitsDescription = benefitsDescription;
        this.subscriptionPrice = subscriptionPrice;
    }

    public void updateFromApplication(String introduction,
                                      String representativeImageUrl,
                                      String representativeImageStorageKey) {
        this.introduction = introduction;
        this.representativeImageUrl = representativeImageUrl;
        this.representativeImageStorageKey = representativeImageStorageKey;
    }
}