package com.white.handdam.subscription.dto.response;

import com.white.handdam.subscription.entity.SubscriptionLevel;

public record SubscriptionPlanResponse(
        Long creatorId,
        SubscriptionLevel subscriptionLevel,
        Integer price,
        boolean available,
        String benefitsDescription
) {

    public static SubscriptionPlanResponse free(Long creatorId) {
        return new SubscriptionPlanResponse(
                creatorId,
                SubscriptionLevel.FREE,
                0,
                true,
                null
        );
    }

    public static SubscriptionPlanResponse paid(
            Long creatorId,
            Integer price,
            boolean available,
            String benefitsDescription
    ) {
        return new SubscriptionPlanResponse(
                creatorId,
                SubscriptionLevel.PAID,
                price,
                available,
                benefitsDescription
        );
    }
}
