package com.white.handdam.subscription.event;

import java.time.Instant;

/**
 * PAID + CANCEL_SCHEDULED subscription is close to its current period end.
 *
 * <p>Published by the subscription expiring-soon transaction service after the target
 * subscription is locked and revalidated. Notification listeners consume this event with
 * {@code @TransactionalEventListener(AFTER_COMMIT)}, so a notification is not created if the
 * publishing transaction rolls back.
 *
 * <p>{@code subscriberId} is the notification recipient. {@code subscriptionId} is the
 * notification reference target. {@code creatorId} is included only as domain context for
 * consumers that need it; listeners must not load sensitive payment data from this event.
 *
 * <p>The scheduler publishes this event for subscriptions whose {@code currentPeriodEndAt}
 * falls within the configured target date. For example, with {@code days-before=3}, a run on
 * July 21 in the configured zone targets subscriptions expiring on July 24 in that zone.
 */
public record SubscriptionExpiringSoonEvent(
        Long subscriptionId,
        Long subscriberId,
        Long creatorId,
        Instant currentPeriodEndAt
) {
}
