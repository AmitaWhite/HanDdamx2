package com.white.handdam.subscription.service;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.event.SubscriptionExpiringSoonEvent;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpiringSoonTransactionServiceTest {

    private static final Long SUBSCRIPTION_ID = 10L;
    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant TARGET_START = Instant.parse("2026-08-13T00:00:00Z");
    private static final Instant TARGET_END = Instant.parse("2026-08-14T00:00:00Z");
    private static final Instant PERIOD_END_AT = Instant.parse("2026-08-13T12:00:00Z");

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SubscriptionExpiringSoonTransactionService transactionService;

    @Test
    @DisplayName("publishExpiringSoonEvent locks target and publishes event")
    void publishExpiringSoonEventLocksTargetAndPublishesEvent() {
        Subscription subscription = scheduledPaidSubscription(PERIOD_END_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean published = transactionService.publishExpiringSoonEvent(
                SUBSCRIPTION_ID,
                PERIOD_END_AT,
                TARGET_START,
                TARGET_END
        );

        ArgumentCaptor<SubscriptionExpiringSoonEvent> eventCaptor =
                ArgumentCaptor.forClass(SubscriptionExpiringSoonEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        SubscriptionExpiringSoonEvent event = eventCaptor.getValue();
        assertThat(published).isTrue();
        assertThat(event.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(event.subscriberId()).isEqualTo(SUBSCRIBER_ID);
        assertThat(event.creatorId()).isEqualTo(CREATOR_ID);
        assertThat(event.currentPeriodEndAt()).isEqualTo(PERIOD_END_AT);
    }

    @Test
    @DisplayName("publishExpiringSoonEvent does not change subscription state")
    void publishExpiringSoonEventDoesNotChangeSubscriptionState() {
        Subscription subscription = scheduledPaidSubscription(PERIOD_END_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        transactionService.publishExpiringSoonEvent(SUBSCRIPTION_ID, PERIOD_END_AT, TARGET_START, TARGET_END);

        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        assertThat(subscription.getSubscriptionPriceSnapshot()).isEqualTo(15000);
        assertThat(subscription.isAutoRenew()).isFalse();
        assertThat(subscription.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(subscription.getCurrentPeriodStartAt()).isEqualTo(STARTED_AT);
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(PERIOD_END_AT);
        assertThat(subscription.getCancelScheduledAt()).isEqualTo(STARTED_AT.plusSeconds(60));
    }

    @Test
    @DisplayName("publishExpiringSoonEvent skips active paid subscription after lock")
    void publishExpiringSoonEventSkipsActivePaid() {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", PERIOD_END_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean published = transactionService.publishExpiringSoonEvent(
                SUBSCRIPTION_ID,
                PERIOD_END_AT,
                TARGET_START,
                TARGET_END
        );

        assertThat(published).isFalse();
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("publishExpiringSoonEvent skips free subscription after lock")
    void publishExpiringSoonEventSkipsFree() {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean published = transactionService.publishExpiringSoonEvent(
                SUBSCRIPTION_ID,
                PERIOD_END_AT,
                TARGET_START,
                TARGET_END
        );

        assertThat(published).isFalse();
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("publishExpiringSoonEvent skips subscription when current period end changed after query")
    void publishExpiringSoonEventSkipsChangedPeriodEnd() {
        Subscription subscription = scheduledPaidSubscription(PERIOD_END_AT.plusSeconds(1));
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean published = transactionService.publishExpiringSoonEvent(
                SUBSCRIPTION_ID,
                PERIOD_END_AT,
                TARGET_START,
                TARGET_END
        );

        assertThat(published).isFalse();
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("publishExpiringSoonEvent includes targetStart")
    void publishExpiringSoonEventIncludesTargetStart() {
        Subscription subscription = scheduledPaidSubscription(TARGET_START);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean published = transactionService.publishExpiringSoonEvent(
                SUBSCRIPTION_ID,
                TARGET_START,
                TARGET_START,
                TARGET_END
        );

        assertThat(published).isTrue();
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(SubscriptionExpiringSoonEvent.class));
    }

    @Test
    @DisplayName("publishExpiringSoonEvent excludes targetEnd")
    void publishExpiringSoonEventExcludesTargetEnd() {
        Subscription subscription = scheduledPaidSubscription(TARGET_END);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        boolean published = transactionService.publishExpiringSoonEvent(
                SUBSCRIPTION_ID,
                TARGET_END,
                TARGET_START,
                TARGET_END
        );

        assertThat(published).isFalse();
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("publishExpiringSoonEvent propagates lock acquisition failure and does not publish event")
    void publishExpiringSoonEventPropagatesLockFailure() {
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> transactionService.publishExpiringSoonEvent(
                SUBSCRIPTION_ID,
                PERIOD_END_AT,
                TARGET_START,
                TARGET_END
        )).isInstanceOf(CannotAcquireLockException.class);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("publishExpiringSoonEvent propagates event publish failure")
    void publishExpiringSoonEventPropagatesEventPublishFailure() {
        Subscription subscription = scheduledPaidSubscription(PERIOD_END_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        doThrow(new IllegalStateException("event bus down"))
                .when(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(SubscriptionExpiringSoonEvent.class));

        assertThatThrownBy(() -> transactionService.publishExpiringSoonEvent(
                SUBSCRIPTION_ID,
                PERIOD_END_AT,
                TARGET_START,
                TARGET_END
        )).isInstanceOf(IllegalStateException.class);
    }

    private Subscription scheduledPaidSubscription(Instant periodEndAt) {
        Subscription subscription = Subscription.createPaid(SUBSCRIBER_ID, CREATOR_ID, 15000, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", periodEndAt);
        subscription.scheduleCancellation(STARTED_AT.plusSeconds(60));
        return subscription;
    }
}
