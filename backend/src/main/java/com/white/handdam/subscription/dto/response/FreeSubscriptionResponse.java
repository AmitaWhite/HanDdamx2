package com.white.handdam.subscription.dto.response;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;

import java.time.Instant;

public record FreeSubscriptionResponse(
        Long subscriptionId,
        Long creatorId,
        SubscriptionLevel subscriptionLevel,
        SubscriptionStatus status,
        Instant startedAt
) {

    public static FreeSubscriptionResponse from(Subscription subscription) {
        return new FreeSubscriptionResponse(
                subscription.getId(),
                subscription.getCreatorId(),
                subscription.getSubscriptionLevel(),
                subscription.getStatus(),
                subscription.getStartedAt()
        );
    }
}
