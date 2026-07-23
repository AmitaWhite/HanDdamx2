package com.white.handdam.subscription.dto.response;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;

import java.time.Instant;

public record SubscriptionStatusResponse(
        Long creatorId,
        boolean subscribed,
        SubscriptionLevel subscriptionLevel,
        SubscriptionStatus status,
        Instant startedAt,
        Instant currentPeriodEndAt
) {

    public static SubscriptionStatusResponse subscribed(Subscription subscription) {
        return new SubscriptionStatusResponse(
                subscription.getCreatorId(),
                true,
                subscription.getSubscriptionLevel(),
                subscription.getStatus(),
                subscription.getStartedAt(),
                subscription.getCurrentPeriodEndAt()
        );
    }

    public static SubscriptionStatusResponse notSubscribed(Long creatorId) {
        return new SubscriptionStatusResponse(
                creatorId,
                false,
                null,
                null,
                null,
                null
        );
    }
}
