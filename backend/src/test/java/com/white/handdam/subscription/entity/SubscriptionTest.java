package com.white.handdam.subscription.entity;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant CANCEL_REQUESTED_AT = Instant.parse("2026-07-20T00:00:00Z");

    @Test
    @DisplayName("createFree creates an active free subscription")
    void createFreeCreatesActiveFreeSubscription() {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);

        assertThat(subscription.getSubscriberId()).isEqualTo(SUBSCRIBER_ID);
        assertThat(subscription.getCreatorId()).isEqualTo(CREATOR_ID);
        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.isFree()).isTrue();
    }

    @Test
    @DisplayName("createFree leaves paid subscription fields empty")
    void createFreeKeepsPaymentAndPeriodFieldsEmpty() {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);

        assertThat(subscription.isAutoRenew()).isFalse();
        assertThat(subscription.getSubscriptionPriceSnapshot()).isNull();
        assertThat(subscription.getCurrentPeriodStartAt()).isNull();
        assertThat(subscription.getCurrentPeriodEndAt()).isNull();
        assertThat(subscription.getCancelScheduledAt()).isNull();
    }

    @Test
    @DisplayName("createFree uses the provided startedAt")
    void createFreeUsesGivenStartedAt() {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);

        assertThat(subscription.getStartedAt()).isEqualTo(STARTED_AT);
    }

    @Test
    @DisplayName("createFree rejects self subscription")
    void createFreeRejectsSelfSubscription() {
        assertThatThrownBy(() -> Subscription.createFree(SUBSCRIBER_ID, SUBSCRIBER_ID, STARTED_AT))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED)
                );
    }

    @Test
    @DisplayName("createFree rejects null subscriberId")
    void createFreeRejectsNullSubscriberId() {
        assertThatThrownBy(() -> Subscription.createFree(null, CREATOR_ID, STARTED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("subscriberId must not be null");
    }

    @Test
    @DisplayName("createFree rejects null creatorId")
    void createFreeRejectsNullCreatorId() {
        assertThatThrownBy(() -> Subscription.createFree(SUBSCRIBER_ID, null, STARTED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creatorId must not be null");
    }

    @Test
    @DisplayName("createFree rejects null startedAt")
    void createFreeRejectsNullStartedAt() {
        assertThatThrownBy(() -> Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("startedAt must not be null");
    }

    @Test
    @DisplayName("createPaid creates an active paid subscription for one month")
    void createPaidCreatesActivePaidSubscription() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);

        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getSubscriptionPriceSnapshot()).isEqualTo(15000);
        assertThat(subscription.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(subscription.getCurrentPeriodStartAt()).isEqualTo(STARTED_AT);
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(
                STARTED_AT.atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant()
        );
        assertThat(subscription.isAutoRenew()).isFalse();
        assertThat(subscription.getCancelScheduledAt()).isNull();
    }

    @Test
    @DisplayName("upgradeToPaid changes an existing free subscription to paid without changing startedAt")
    void upgradeToPaidChangesFreeSubscriptionToPaid() {
        Instant approvedAt = Instant.parse("2026-07-15T00:00:00Z");
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);

        subscription.upgradeToPaid(15000, approvedAt);

        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(subscription.getCurrentPeriodStartAt()).isEqualTo(approvedAt);
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(
                approvedAt.atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant()
        );
        assertThat(subscription.getSubscriptionPriceSnapshot()).isEqualTo(15000);
    }

    @Test
    @DisplayName("scheduleCancellation changes active paid subscription to cancel scheduled")
    void scheduleCancellationChangesActivePaidSubscription() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        Instant periodEndAt = subscription.getCurrentPeriodEndAt();

        subscription.scheduleCancellation(CANCEL_REQUESTED_AT);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        assertThat(subscription.getCancelScheduledAt()).isEqualTo(CANCEL_REQUESTED_AT);
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(periodEndAt);
    }

    @Test
    @DisplayName("scheduleCancellation is idempotent when already scheduled")
    void scheduleCancellationIsIdempotent() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        subscription.scheduleCancellation(CANCEL_REQUESTED_AT);

        subscription.scheduleCancellation(Instant.parse("2026-07-21T00:00:00Z"));

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        assertThat(subscription.getCancelScheduledAt()).isEqualTo(CANCEL_REQUESTED_AT);
    }

    @Test
    @DisplayName("scheduleCancellation rejects free subscription")
    void scheduleCancellationRejectsFreeSubscription() {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);

        assertThatThrownBy(() -> subscription.scheduleCancellation(CANCEL_REQUESTED_AT))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.FREE_SUBSCRIPTION_CANNOT_BE_CANCEL_SCHEDULED)
                );
    }

    @Test
    @DisplayName("revokeCancellationSchedule changes scheduled paid subscription to active")
    void revokeCancellationScheduleChangesScheduledPaidSubscription() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        Instant periodEndAt = subscription.getCurrentPeriodEndAt();
        subscription.scheduleCancellation(CANCEL_REQUESTED_AT);

        subscription.revokeCancellationSchedule();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCancelScheduledAt()).isNull();
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(periodEndAt);
    }

    @Test
    @DisplayName("revokeCancellationSchedule is idempotent for active paid subscription without schedule")
    void revokeCancellationScheduleIsIdempotent() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);

        subscription.revokeCancellationSchedule();

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCancelScheduledAt()).isNull();
    }

    @Test
    @DisplayName("revokeCancellationSchedule rejects free subscription")
    void revokeCancellationScheduleRejectsFreeSubscription() {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);

        assertThatThrownBy(() -> subscription.revokeCancellationSchedule())
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.FREE_SUBSCRIPTION_CANNOT_BE_CANCEL_SCHEDULED)
                );
    }

    @Test
    @DisplayName("convertToFreeAfterPeriodEnd changes expired cancel-scheduled paid subscription to active free")
    void convertToFreeAfterPeriodEndChangesExpiredPaidSubscriptionToFree() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        Instant periodEndAt = subscription.getCurrentPeriodEndAt();
        subscription.scheduleCancellation(CANCEL_REQUESTED_AT);
        Instant now = periodEndAt.plusSeconds(1);

        subscription.convertToFreeAfterPeriodEnd(now);

        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getSubscriptionPriceSnapshot()).isNull();
        assertThat(subscription.getCurrentPeriodStartAt()).isNull();
        assertThat(subscription.getCurrentPeriodEndAt()).isNull();
        assertThat(subscription.getCancelScheduledAt()).isNull();
        assertThat(subscription.isAutoRenew()).isFalse();
        assertThat(subscription.getStartedAt()).isEqualTo(STARTED_AT);
    }

    @Test
    @DisplayName("convertToFreeAfterPeriodEnd allows currentPeriodEndAt equal to now")
    void convertToFreeAfterPeriodEndAllowsBoundaryTime() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        Instant periodEndAt = subscription.getCurrentPeriodEndAt();
        subscription.scheduleCancellation(CANCEL_REQUESTED_AT);

        subscription.convertToFreeAfterPeriodEnd(periodEndAt);

        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("convertToFreeAfterPeriodEnd rejects a free subscription")
    void convertToFreeAfterPeriodEndRejectsFreeSubscription() {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);

        assertThatThrownBy(() -> subscription.convertToFreeAfterPeriodEnd(CANCEL_REQUESTED_AT))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_EXPIRATION_NOT_ALLOWED)
                );
    }

    @Test
    @DisplayName("convertToFreeAfterPeriodEnd rejects active paid subscription")
    void convertToFreeAfterPeriodEndRejectsActivePaidSubscription() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        Instant periodEndAt = subscription.getCurrentPeriodEndAt();

        assertThatThrownBy(() -> subscription.convertToFreeAfterPeriodEnd(periodEndAt))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_EXPIRATION_NOT_ALLOWED)
                );
    }

    @Test
    @DisplayName("convertToFreeAfterPeriodEnd rejects missing current period end")
    void convertToFreeAfterPeriodEndRejectsMissingCurrentPeriodEnd() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        subscription.scheduleCancellation(CANCEL_REQUESTED_AT);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", null);

        assertThatThrownBy(() -> subscription.convertToFreeAfterPeriodEnd(CANCEL_REQUESTED_AT))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_STATE_CONFLICT)
                );
    }

    @Test
    @DisplayName("convertToFreeAfterPeriodEnd rejects before current period end")
    void convertToFreeAfterPeriodEndRejectsBeforePeriodEnd() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        Instant periodEndAt = subscription.getCurrentPeriodEndAt();
        subscription.scheduleCancellation(CANCEL_REQUESTED_AT);

        assertThatThrownBy(() -> subscription.convertToFreeAfterPeriodEnd(periodEndAt.minusSeconds(1)))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_PERIOD_NOT_ENDED)
                );
    }

    @Test
    @DisplayName("isExpiredCancelScheduledPaid returns true only for expired cancel scheduled paid subscription")
    void isExpiredCancelScheduledPaidChecksTargetCondition() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        Instant periodEndAt = subscription.getCurrentPeriodEndAt();

        assertThat(subscription.isExpiredCancelScheduledPaid(periodEndAt)).isFalse();

        subscription.scheduleCancellation(CANCEL_REQUESTED_AT);

        assertThat(subscription.isExpiredCancelScheduledPaid(periodEndAt.minusSeconds(1))).isFalse();
        assertThat(subscription.isExpiredCancelScheduledPaid(periodEndAt)).isTrue();
    }
}
