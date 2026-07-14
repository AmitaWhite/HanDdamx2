package com.white.handdam.subscription.dto.response;

import com.white.handdam.member.entity.Member;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;

import java.time.Instant;

public record MySubscriptionResponse(
        Long subscriptionId,
        Long creatorId,
        String creatorNickname,
        String creatorProfileImageUrl,
        SubscriptionLevel subscriptionLevel,
        SubscriptionStatus status,
        Instant startedAt,
        Instant currentPeriodEndAt,
        Instant cancelScheduledAt
) {

    public static MySubscriptionResponse from(Subscription subscription, Member creator) {
        return new MySubscriptionResponse(
                subscription.getId(),
                subscription.getCreatorId(),
                creator.getNickname(),
                creator.getProfileImageUrl(),
                subscription.getSubscriptionLevel(),
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getCurrentPeriodEndAt(),
                subscription.getCancelScheduledAt()
        );
    }
}
