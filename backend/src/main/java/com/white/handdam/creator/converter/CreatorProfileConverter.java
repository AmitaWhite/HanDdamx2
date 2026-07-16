package com.white.handdam.creator.converter;

import com.white.handdam.creator.dto.response.CreatorProfileResponse;
import com.white.handdam.creator.entity.CreatorProfile;
import com.white.handdam.member.entity.Member;
import com.white.handdam.subscription.entity.Subscription;

/**
 * CreatorProfile 엔티티 → DTO 변환
 */
public final class CreatorProfileConverter {

    private CreatorProfileConverter() {}

    public static CreatorProfileResponse toResponse(
            CreatorProfile profile, // 크리에이터 프로필 엔티티
            Member member, //크리에이터 Member 엔티티
            Subscription subscription, // 요청자의 구독 정보
            long subscriberCount, // 총 구독자 수
            long projectCount, // 프로젝트 수
            long feedCount, // 피드 수
            boolean isMine // 요청자(크리에이터 본인 여부)
    ) {
        // 구독 중이면 등급/상태 반환, 아니면 null
        String subscriptionLevel = subscription != null
                ? subscription.getSubscriptionLevel().name()
                : null;
        String subscriptionStatus = subscription != null
                ? subscription.getStatus().name()
                : null;

        return new CreatorProfileResponse(
                profile.getId(),
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                profile.getCoverImageUrl(),
                profile.getRepresentativeImageUrl(),
                profile.getIntroduction(),
                profile.getBenefitsDescription(),
                profile.getSubscriptionPrice(),
                subscriptionLevel,
                subscriptionStatus,
                subscriberCount,
                projectCount,
                feedCount,
                isMine
        );
    }
}