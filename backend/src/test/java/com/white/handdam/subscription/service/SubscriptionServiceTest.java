package com.white.handdam.subscription.service;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.subscription.dto.response.FreeSubscriptionResponse;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Long SUBSCRIPTION_ID = 10L;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("createFreeSubscription saves a free subscription when no subscription exists")
    void createFreeSubscriptionSavesFreeSubscriptionWhenNotExists() {
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription subscription = invocation.getArgument(0);
                    ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
                    return subscription;
                });

        FreeSubscriptionResponse response = subscriptionService.createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription savedSubscription = captor.getValue();

        assertThat(savedSubscription.getSubscriberId()).isEqualTo(SUBSCRIBER_ID);
        assertThat(savedSubscription.getCreatorId()).isEqualTo(CREATOR_ID);
        assertThat(savedSubscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(savedSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(savedSubscription.isAutoRenew()).isFalse();
        assertThat(savedSubscription.getSubscriptionPriceSnapshot()).isNull();
        assertThat(savedSubscription.getCurrentPeriodStartAt()).isNull();
        assertThat(savedSubscription.getCurrentPeriodEndAt()).isNull();
        assertThat(savedSubscription.getCancelScheduledAt()).isNull();
        assertThat(savedSubscription.getStartedAt()).isNotNull();

        assertThat(response.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(response.creatorId()).isEqualTo(CREATOR_ID);
        assertThat(response.subscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.startedAt()).isEqualTo(savedSubscription.getStartedAt());
    }

    @Test
    @DisplayName("createFreeSubscription rejects an existing free subscription")
    void createFreeSubscriptionRejectsExistingFreeSubscription() {
        Subscription existingSubscription = Subscription.createFree(
                SUBSCRIBER_ID,
                CREATOR_ID,
                Instant.parse("2026-07-13T00:00:00Z")
        );
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(existingSubscription));

        assertThatThrownBy(() -> subscriptionService.createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS)
                );
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("createFreeSubscription rejects an existing paid subscription")
    void createFreeSubscriptionRejectsExistingPaidSubscription() {
        Subscription existingPaidSubscription = mock(Subscription.class);
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(existingPaidSubscription));

        assertThatThrownBy(() -> subscriptionService.createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS)
                );
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("cancelFreeSubscription deletes a free subscription")
    void cancelFreeSubscriptionDeletesFreeSubscription() {
        Subscription subscription = Subscription.createFree(
                SUBSCRIBER_ID,
                CREATOR_ID,
                Instant.parse("2026-07-13T00:00:00Z")
        );
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(subscription));

        subscriptionService.cancelFreeSubscription(SUBSCRIBER_ID, CREATOR_ID);

        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    @DisplayName("cancelFreeSubscription rejects a missing subscription")
    void cancelFreeSubscriptionRejectsMissingSubscription() {
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.cancelFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.FREE_SUBSCRIPTION_NOT_FOUND)
                );
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }

    @Test
    @DisplayName("cancelFreeSubscription does not delete a paid subscription")
    void cancelFreeSubscriptionRejectsPaidSubscription() {
        Subscription paidSubscription = mock(Subscription.class);
        when(paidSubscription.isFree()).thenReturn(false);
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(paidSubscription));

        assertThatThrownBy(() -> subscriptionService.cancelFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.PAID_SUBSCRIPTION_CANNOT_BE_CANCELED_AS_FREE)
                );
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }
}
