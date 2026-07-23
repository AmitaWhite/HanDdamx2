package com.white.handdam.subscription.service;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationTransactionServiceTest {

    private static final Long SUBSCRIPTION_ID = 10L;
    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant PERIOD_END_AT = Instant.parse("2026-08-13T00:00:00Z");
    private static final Instant NOW = PERIOD_END_AT;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionExpirationTransactionService transactionService;

    @Test
    @DisplayName("convertExpiredSubscriptionToFree locks and converts target subscription")
    void convertExpiredSubscriptionToFreeLocksAndConvertsTarget() {
        Subscription subscription = scheduledPaidSubscription(PERIOD_END_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean converted = transactionService.convertExpiredSubscriptionToFree(SUBSCRIPTION_ID, NOW);

        assertThat(converted).isTrue();
        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getSubscriptionPriceSnapshot()).isNull();
        assertThat(subscription.getCurrentPeriodStartAt()).isNull();
        assertThat(subscription.getCurrentPeriodEndAt()).isNull();
        assertThat(subscription.getCancelScheduledAt()).isNull();
        assertThat(subscription.isAutoRenew()).isFalse();
        verify(subscriptionRepository).findByIdForUpdate(SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("convertExpiredSubscriptionToFree propagates lock acquisition failure")
    void convertExpiredSubscriptionToFreePropagatesLockAcquisitionFailure() {
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> transactionService.convertExpiredSubscriptionToFree(SUBSCRIPTION_ID, NOW))
                .isInstanceOf(CannotAcquireLockException.class);

        verify(subscriptionRepository).findByIdForUpdate(SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("convertExpiredSubscriptionToFree skips missing subscription")
    void convertExpiredSubscriptionToFreeSkipsMissingSubscription() {
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        boolean converted = transactionService.convertExpiredSubscriptionToFree(SUBSCRIPTION_ID, NOW);

        assertThat(converted).isFalse();
        verify(subscriptionRepository).findByIdForUpdate(SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("convertExpiredSubscriptionToFree skips already processed subscription after lock")
    void convertExpiredSubscriptionToFreeSkipsAlreadyProcessedSubscription() {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean converted = transactionService.convertExpiredSubscriptionToFree(SUBSCRIPTION_ID, NOW);

        assertThat(converted).isFalse();
        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).findByIdForUpdate(SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("convertExpiredSubscriptionToFree rechecks status after lock")
    void convertExpiredSubscriptionToFreeRechecksStatusAfterLock() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", PERIOD_END_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean converted = transactionService.convertExpiredSubscriptionToFree(SUBSCRIPTION_ID, NOW);

        assertThat(converted).isFalse();
        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).findByIdForUpdate(SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("convertExpiredSubscriptionToFree skips not-yet-expired subscription after lock")
    void convertExpiredSubscriptionToFreeSkipsNotYetExpiredSubscriptionAfterLock() {
        Subscription subscription = scheduledPaidSubscription(PERIOD_END_AT.plusSeconds(1));
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean converted = transactionService.convertExpiredSubscriptionToFree(SUBSCRIPTION_ID, NOW);

        assertThat(converted).isFalse();
        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        verify(subscriptionRepository).findByIdForUpdate(SUBSCRIPTION_ID);
    }

    private Subscription scheduledPaidSubscription(Instant periodEndAt) {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", periodEndAt);
        subscription.scheduleCancellation(STARTED_AT.plusSeconds(60));
        return subscription;
    }
}
