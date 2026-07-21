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
 * <p>A SUBSCRIPTION_EXPIRING notification must be stored at most once for the same
 * {@code subscriptionId} and {@code currentPeriodEndAt}. If a later paid period changes
 * {@code currentPeriodEndAt}, a new notification may be created for that new period.
 */
public record SubscriptionExpiringSoonEvent(
        Long subscriptionId,
        Long subscriberId,
        Long creatorId,
        Instant currentPeriodEndAt
) {
}
