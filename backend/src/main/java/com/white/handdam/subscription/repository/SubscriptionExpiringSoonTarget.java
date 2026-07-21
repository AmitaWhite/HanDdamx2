package com.white.handdam.subscription.repository;

import java.time.Instant;

public record SubscriptionExpiringSoonTarget(
        Long subscriptionId,
        Instant currentPeriodEndAt
) {
}
