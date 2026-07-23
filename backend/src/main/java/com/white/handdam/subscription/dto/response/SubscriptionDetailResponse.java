package com.white.handdam.subscription.dto.response;

import com.white.handdam.member.entity.Member;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionDetailResponse(
        Long subscriptionId,
        Long creatorId,
        String creatorNickname,
        String creatorProfileImageUrl,
        SubscriptionLevel subscriptionLevel,
        SubscriptionStatus status,
        Integer subscriptionPriceSnapshot,
        Instant startedAt,
        Instant currentPeriodStartAt,
        Instant currentPeriodEndAt,
        Instant cancelScheduledAt
) {

    public static SubscriptionDetailResponse from(Subscription subscription, Member creator) {
        return new SubscriptionDetailResponse(
                subscription.getId(),
                subscription.getCreatorId(),
                creator.getNickname(),
                creator.getProfileImageUrl(),
                subscription.getSubscriptionLevel(),
                subscription.getStatus(),
                subscription.getSubscriptionPriceSnapshot(),
                subscription.getStartedAt(),
                subscription.getCurrentPeriodStartAt(),
                subscription.getCurrentPeriodEndAt(),
                subscription.getCancelScheduledAt()
        );
    }
}
