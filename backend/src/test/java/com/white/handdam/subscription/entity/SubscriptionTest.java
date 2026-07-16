package com.white.handdam.subscription.entity;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:00Z");

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
}
