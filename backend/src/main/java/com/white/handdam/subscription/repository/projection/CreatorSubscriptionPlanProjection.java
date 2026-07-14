package com.white.handdam.subscription.repository.projection;

public record CreatorSubscriptionPlanProjection(
        Long memberId,
        Integer subscriptionPrice,
        String benefitsDescription
) {
}
