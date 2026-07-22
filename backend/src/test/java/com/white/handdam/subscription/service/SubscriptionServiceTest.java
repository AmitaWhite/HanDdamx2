package com.white.handdam.subscription.service;

import com.white.handdam.creator.entity.CreatorProfile;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.creator.repository.CreatorProfileRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.subscription.dto.response.FreeSubscriptionResponse;
import com.white.handdam.subscription.dto.response.MySubscriptionResponse;
import com.white.handdam.subscription.dto.response.SubscriptionPlanResponse;
import com.white.handdam.subscription.dto.response.SubscriptionStatusResponse;
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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long OTHER_SUBSCRIBER_ID = 9L;
    private static final Long CREATOR_ID = 2L;
    private static final Long SECOND_CREATOR_ID = 3L;
    private static final Long SUBSCRIPTION_ID = 10L;
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant LATER_STARTED_AT = Instant.parse("2026-07-14T00:00:00Z");
    private static final Instant PERIOD_END_AT = Instant.parse("2026-08-14T00:00:00Z");
    private static final Instant CANCEL_SCHEDULED_AT = Instant.parse("2026-07-20T00:00:00Z");

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CreatorProfileRepository creatorProfileRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("createFreeSubscription saves a free subscription when no subscription exists")
    void createFreeSubscriptionSavesFreeSubscriptionWhenNotExists() {
        mockSubscribableCreator(CREATOR_ID);
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
    @DisplayName("createFreeSubscription rejects null subscriberId before repository access")
    void createFreeSubscriptionRejectsNullSubscriberId() {
        assertThatThrownBy(() -> subscriptionService.createFreeSubscription(null, CREATOR_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("subscriberId must not be null");

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("createFreeSubscription rejects null creatorId before repository access")
    void createFreeSubscriptionRejectsNullCreatorId() {
        assertThatThrownBy(() -> subscriptionService.createFreeSubscription(SUBSCRIBER_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creatorId must not be null");

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    @DisplayName("createFreeSubscription rejects self subscription before creator lookup")
    void createFreeSubscriptionRejectsSelfSubscription() {
        assertThatThrownBy(() -> subscriptionService.createFreeSubscription(SUBSCRIBER_ID, SUBSCRIBER_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED)
                );

        verifyNoInteractions(memberRepository, creatorProfileRepository, subscriptionRepository);
    }

    @Test
    @DisplayName("createFreeSubscription rejects missing creator member")
    void createFreeSubscriptionRejectsMissingCreatorMember() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(CreatorErrorCode.CREATOR_NOT_FOUND)
                );

        verifyNoInteractions(creatorProfileRepository, subscriptionRepository);
    }

    @Test
    @DisplayName("createFreeSubscription rejects a USER member target")
    void createFreeSubscriptionRejectsUserMemberTarget() {
        assertCreateFreeSubscriptionRejectsNonCreator(Role.USER);
    }

    @Test
    @DisplayName("createFreeSubscription rejects an ADMIN member target")
    void createFreeSubscriptionRejectsAdminMemberTarget() {
        assertCreateFreeSubscriptionRejectsNonCreator(Role.ADMIN);
    }

    @Test
    @DisplayName("createFreeSubscription rejects missing creator profile")
    void createFreeSubscriptionRejectsMissingCreatorProfile() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND)
                );

        verify(subscriptionRepository, never()).findBySubscriberIdAndCreatorId(any(), any());
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("createFreeSubscription rejects an existing free subscription")
    void createFreeSubscriptionRejectsExistingFreeSubscription() {
        Subscription existingSubscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);
        mockSubscribableCreator(CREATOR_ID);
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
        mockSubscribableCreator(CREATOR_ID);
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
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT);
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

    @Test
    @DisplayName("getSubscriptionStatus returns an active free subscription")
    void getSubscriptionStatusReturnsFreeSubscription() {
        Subscription subscription = freeSubscription(SUBSCRIPTION_ID, CREATOR_ID, STARTED_AT);
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(subscription));

        SubscriptionStatusResponse response =
                subscriptionService.getSubscriptionStatus(SUBSCRIBER_ID, CREATOR_ID);

        assertThat(response.creatorId()).isEqualTo(CREATOR_ID);
        assertThat(response.subscribed()).isTrue();
        assertThat(response.subscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.startedAt()).isEqualTo(STARTED_AT);
        assertThat(response.currentPeriodEndAt()).isNull();
    }

    @Test
    @DisplayName("getSubscriptionStatus returns a paid subscription")
    void getSubscriptionStatusReturnsPaidSubscription() {
        Subscription paidSubscription = paidSubscription(CREATOR_ID, SubscriptionStatus.CANCEL_SCHEDULED);
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(paidSubscription));

        SubscriptionStatusResponse response =
                subscriptionService.getSubscriptionStatus(SUBSCRIBER_ID, CREATOR_ID);

        assertThat(response.subscribed()).isTrue();
        assertThat(response.subscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        assertThat(response.currentPeriodEndAt()).isEqualTo(PERIOD_END_AT);
    }

    @Test
    @DisplayName("getSubscriptionStatus returns not subscribed when subscription does not exist")
    void getSubscriptionStatusReturnsNotSubscribed() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(Optional.empty());

        SubscriptionStatusResponse response =
                subscriptionService.getSubscriptionStatus(SUBSCRIBER_ID, CREATOR_ID);

        assertThat(response.creatorId()).isEqualTo(CREATOR_ID);
        assertThat(response.subscribed()).isFalse();
        assertThat(response.subscriptionLevel()).isNull();
        assertThat(response.status()).isNull();
        assertThat(response.startedAt()).isNull();
        assertThat(response.currentPeriodEndAt()).isNull();
    }

    @Test
    @DisplayName("getSubscriptionStatus returns not subscribed for self lookup")
    void getSubscriptionStatusReturnsNotSubscribedForSelfLookup() {
        when(memberRepository.findById(SUBSCRIBER_ID))
                .thenReturn(Optional.of(member(SUBSCRIBER_ID, Role.CREATOR)));

        SubscriptionStatusResponse response =
                subscriptionService.getSubscriptionStatus(SUBSCRIBER_ID, SUBSCRIBER_ID);

        assertThat(response.creatorId()).isEqualTo(SUBSCRIBER_ID);
        assertThat(response.subscribed()).isFalse();
        assertThat(response.subscriptionLevel()).isNull();
        assertThat(response.status()).isNull();
        assertThat(response.startedAt()).isNull();
        assertThat(response.currentPeriodEndAt()).isNull();
        verify(subscriptionRepository, never())
                .findBySubscriberIdAndCreatorId(SUBSCRIBER_ID, SUBSCRIBER_ID);
    }

    @Test
    @DisplayName("getSubscriptionStatus rejects a non creator member")
    void getSubscriptionStatusRejectsNonCreator() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.USER)));

        assertThatThrownBy(() -> subscriptionService.getSubscriptionStatus(SUBSCRIBER_ID, CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> {
                            assertThat(exception.getErrorCode()).isEqualTo(CreatorErrorCode.CREATOR_NOT_FOUND);
                            assertThat(exception).hasMessage(CreatorErrorCode.CREATOR_NOT_FOUND.getMessage());
                        }
                );
    }

    @Test
    @DisplayName("getMySubscriptions returns all subscriptions in repository order with creator info")
    void getMySubscriptionsReturnsSubscriptionsWithCreatorInfo() {
        Subscription paidSubscription = paidSubscription(SECOND_CREATOR_ID, SubscriptionStatus.CANCEL_SCHEDULED);
        Subscription freeSubscription = freeSubscription(SUBSCRIPTION_ID, CREATOR_ID, STARTED_AT);
        when(subscriptionRepository.findBySubscriberIdOrderByStartedAtDesc(SUBSCRIBER_ID))
                .thenReturn(List.of(paidSubscription, freeSubscription));
        when(memberRepository.findAllById(any()))
                .thenReturn(List.of(
                        member(SECOND_CREATOR_ID, Role.CREATOR, "paidCreator", "https://image/paid.png"),
                        member(CREATOR_ID, Role.CREATOR, "freeCreator", "https://image/free.png")
                ));

        List<MySubscriptionResponse> responses = subscriptionService.getMySubscriptions(SUBSCRIBER_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).creatorId()).isEqualTo(SECOND_CREATOR_ID);
        assertThat(responses.get(0).creatorNickname()).isEqualTo("paidCreator");
        assertThat(responses.get(0).creatorProfileImageUrl()).isEqualTo("https://image/paid.png");
        assertThat(responses.get(0).subscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(responses.get(0).status()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        assertThat(responses.get(0).currentPeriodEndAt()).isEqualTo(PERIOD_END_AT);
        assertThat(responses.get(0).cancelScheduledAt()).isEqualTo(CANCEL_SCHEDULED_AT);
        assertThat(responses.get(1).creatorId()).isEqualTo(CREATOR_ID);
        assertThat(responses.get(1).creatorNickname()).isEqualTo("freeCreator");
        assertThat(responses.get(1).subscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
    }

    @Test
    @DisplayName("getMySubscriptions returns an empty list when no subscriptions exist")
    void getMySubscriptionsReturnsEmptyList() {
        when(subscriptionRepository.findBySubscriberIdOrderByStartedAtDesc(SUBSCRIBER_ID))
                .thenReturn(List.of());

        List<MySubscriptionResponse> responses = subscriptionService.getMySubscriptions(SUBSCRIBER_ID);

        assertThat(responses).isEmpty();
        verify(memberRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("getMySubscriptions rejects missing creator member data")
    void getMySubscriptionsRejectsMissingCreatorMemberData() {
        Subscription subscription = freeSubscription(SUBSCRIPTION_ID, CREATOR_ID, STARTED_AT);
        when(subscriptionRepository.findBySubscriberIdOrderByStartedAtDesc(SUBSCRIBER_ID))
                .thenReturn(List.of(subscription));
        when(memberRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> subscriptionService.getMySubscriptions(SUBSCRIBER_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(CreatorErrorCode.CREATOR_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("getSubscription returns detail for the current subscriber")
    void getSubscriptionReturnsDetail() {
        Subscription subscription = realPaidSubscription(SUBSCRIBER_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(memberRepository.findById(CREATOR_ID))
                .thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR, "creator", null)));

        var response = subscriptionService.getSubscription(SUBSCRIBER_ID, SUBSCRIPTION_ID);

        assertThat(response.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(response.creatorId()).isEqualTo(CREATOR_ID);
        assertThat(response.creatorNickname()).isEqualTo("creator");
        assertThat(response.subscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.subscriptionPriceSnapshot()).isEqualTo(15000);
        assertThat(response.currentPeriodEndAt()).isEqualTo(PERIOD_END_AT);
    }

    @Test
    @DisplayName("getSubscription rejects a missing subscription")
    void getSubscriptionRejectsMissingSubscription() {
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscription(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("getSubscription rejects another subscriber subscription")
    void getSubscriptionRejectsOwnerMismatch() {
        Subscription subscription = realPaidSubscription(OTHER_SUBSCRIBER_ID);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.getSubscription(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_OWNER_MISMATCH)
                );
        verify(memberRepository, never()).findById(CREATOR_ID);
    }

    @Test
    @DisplayName("scheduleCancellation locks and changes ACTIVE PAID to CANCEL_SCHEDULED")
    void scheduleCancellationLocksAndSchedulesPaidSubscription() {
        Subscription subscription = realPaidSubscription(SUBSCRIBER_ID);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(memberRepository.findById(CREATOR_ID))
                .thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR, "creator", null)));

        var response = subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        assertThat(subscription.getCancelScheduledAt()).isNotNull();
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(PERIOD_END_AT);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        assertThat(response.cancelScheduledAt()).isEqualTo(subscription.getCancelScheduledAt());
        verify(subscriptionRepository).findByIdForUpdate(SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("scheduleCancellation propagates lock acquisition failure")
    void scheduleCancellationPropagatesLockAcquisitionFailure() {
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOf(CannotAcquireLockException.class);

        verify(memberRepository, never()).findById(any());
    }

    @Test
    @DisplayName("scheduleCancellation rejects a missing subscription")
    void scheduleCancellationRejectsMissingSubscription() {
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("scheduleCancellation rejects another subscriber subscription")
    void scheduleCancellationRejectsOwnerMismatch() {
        Subscription subscription = realPaidSubscription(OTHER_SUBSCRIBER_ID);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_OWNER_MISMATCH)
                );
        verify(memberRepository, never()).findById(CREATOR_ID);
    }

    @Test
    @DisplayName("scheduleCancellation rejects a FREE subscription")
    void scheduleCancellationRejectsFreeSubscription() {
        Subscription subscription = freeSubscription(SUBSCRIPTION_ID, CREATOR_ID, STARTED_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.FREE_SUBSCRIPTION_CANNOT_BE_CANCEL_SCHEDULED)
                );
    }

    @Test
    @DisplayName("scheduleCancellation is idempotent for an already scheduled paid subscription")
    void scheduleCancellationIsIdempotent() {
        Subscription subscription = realPaidSubscription(SUBSCRIBER_ID);
        subscription.scheduleCancellation(CANCEL_SCHEDULED_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(memberRepository.findById(CREATOR_ID))
                .thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR, "creator", null)));

        var response = subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.CANCEL_SCHEDULED);
        assertThat(response.cancelScheduledAt()).isEqualTo(CANCEL_SCHEDULED_AT);
    }

    @Test
    @DisplayName("scheduleCancellation rejects paid subscription without current period end")
    void scheduleCancellationRejectsMissingPeriodEnd() {
        Subscription subscription = realPaidSubscription(SUBSCRIBER_ID);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", null);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SUBSCRIPTION_STATE_CONFLICT)
                );
    }

    @Test
    @DisplayName("revokeCancellationSchedule locks and changes CANCEL_SCHEDULED to ACTIVE")
    void revokeCancellationScheduleLocksAndRevokes() {
        Subscription subscription = realPaidSubscription(SUBSCRIBER_ID);
        subscription.scheduleCancellation(CANCEL_SCHEDULED_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(memberRepository.findById(CREATOR_ID))
                .thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR, "creator", null)));

        var response = subscriptionService.revokeCancellationSchedule(SUBSCRIBER_ID, SUBSCRIPTION_ID);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCancelScheduledAt()).isNull();
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(PERIOD_END_AT);
        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.cancelScheduledAt()).isNull();
        verify(subscriptionRepository).findByIdForUpdate(SUBSCRIPTION_ID);
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }

    @Test
    @DisplayName("revokeCancellationSchedule propagates lock acquisition failure")
    void revokeCancellationSchedulePropagatesLockAcquisitionFailure() {
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> subscriptionService.revokeCancellationSchedule(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOf(CannotAcquireLockException.class);

        verify(memberRepository, never()).findById(any());
        verify(subscriptionRepository, never()).delete(any(Subscription.class));
    }

    @Test
    @DisplayName("revokeCancellationSchedule is idempotent for active paid subscription")
    void revokeCancellationScheduleIsIdempotent() {
        Subscription subscription = realPaidSubscription(SUBSCRIBER_ID);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(memberRepository.findById(CREATOR_ID))
                .thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR, "creator", null)));

        var response = subscriptionService.revokeCancellationSchedule(SUBSCRIBER_ID, SUBSCRIPTION_ID);

        assertThat(response.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.cancelScheduledAt()).isNull();
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(PERIOD_END_AT);
    }

    @Test
    @DisplayName("revokeCancellationSchedule rejects a FREE subscription")
    void revokeCancellationScheduleRejectsFreeSubscription() {
        Subscription subscription = freeSubscription(SUBSCRIPTION_ID, CREATOR_ID, STARTED_AT);
        when(subscriptionRepository.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> subscriptionService.revokeCancellationSchedule(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.FREE_SUBSCRIPTION_CANNOT_BE_CANCEL_SCHEDULED)
                );
    }

    @Test
    @DisplayName("getSubscriptionPlans returns free and active paid plans")
    void getSubscriptionPlansReturnsFreeAndActivePaidPlans() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID))
                .thenReturn(Optional.of(creatorProfile(CREATOR_ID, 10000, "monthly benefits")));

        List<SubscriptionPlanResponse> responses = subscriptionService.getSubscriptionPlans(CREATOR_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).subscriptionLevel()).isEqualTo(SubscriptionLevel.FREE);
        assertThat(responses.get(0).price()).isZero();
        assertThat(responses.get(0).available()).isTrue();
        assertThat(responses.get(0).benefitsDescription()).isNull();
        assertThat(responses.get(1).subscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(responses.get(1).price()).isEqualTo(10000);
        assertThat(responses.get(1).available()).isTrue();
        assertThat(responses.get(1).benefitsDescription()).isEqualTo("monthly benefits");
    }

    @Test
    @DisplayName("getSubscriptionPlans includes an inactive paid plan when price is zero")
    void getSubscriptionPlansIncludesInactivePaidPlan() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID))
                .thenReturn(Optional.of(creatorProfile(CREATOR_ID, 0, null)));

        List<SubscriptionPlanResponse> responses = subscriptionService.getSubscriptionPlans(CREATOR_ID);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(1).subscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(responses.get(1).price()).isZero();
        assertThat(responses.get(1).available()).isFalse();
        assertThat(responses.get(1).benefitsDescription()).isNull();
    }

    @Test
    @DisplayName("getSubscriptionPlans rejects missing creator member")
    void getSubscriptionPlansRejectsMissingCreatorMember() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionPlans(CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> {
                            assertThat(exception.getErrorCode()).isEqualTo(CreatorErrorCode.CREATOR_NOT_FOUND);
                            assertThat(exception).hasMessage(CreatorErrorCode.CREATOR_NOT_FOUND.getMessage());
                        }
                );
        verify(creatorProfileRepository, never()).findByMemberId(any());
    }

    @Test
    @DisplayName("getSubscriptionPlans rejects a non creator member")
    void getSubscriptionPlansRejectsNonCreatorMember() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.USER)));

        assertThatThrownBy(() -> subscriptionService.getSubscriptionPlans(CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> {
                            assertThat(exception.getErrorCode()).isEqualTo(CreatorErrorCode.CREATOR_NOT_FOUND);
                            assertThat(exception).hasMessage(CreatorErrorCode.CREATOR_NOT_FOUND.getMessage());
                        }
                );
        verify(creatorProfileRepository, never()).findByMemberId(any());
    }

    @Test
    @DisplayName("getSubscriptionPlans rejects missing creator profile")
    void getSubscriptionPlansRejectsMissingCreatorProfile() {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getSubscriptionPlans(CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> {
                            assertThat(exception.getErrorCode()).isEqualTo(
                                    CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND
                            );
                            assertThat(exception).hasMessage(
                                    CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND.getMessage()
                            );
                        }
                );
        verify(creatorProfileRepository).findByMemberId(CREATOR_ID);
    }

    private CreatorProfile creatorProfile(Long memberId, int subscriptionPrice, String benefitsDescription) {
        return CreatorProfile.builder()
                .memberId(memberId)
                .subscriptionPrice(subscriptionPrice)
                .benefitsDescription(benefitsDescription)
                .build();
    }

    private void mockSubscribableCreator(Long creatorId) {
        when(memberRepository.findById(creatorId)).thenReturn(Optional.of(member(creatorId, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(creatorId))
                .thenReturn(Optional.of(creatorProfile(creatorId, 10000, "monthly benefits")));
    }

    private void assertCreateFreeSubscriptionRejectsNonCreator(Role role) {
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, role)));

        assertThatThrownBy(() -> subscriptionService.createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(CreatorErrorCode.CREATOR_NOT_FOUND)
                );

        verifyNoInteractions(creatorProfileRepository, subscriptionRepository);
    }

    private Subscription freeSubscription(Long subscriptionId, Long creatorId, Instant startedAt) {
        Subscription subscription = Subscription.createFree(SUBSCRIBER_ID, creatorId, startedAt);
        ReflectionTestUtils.setField(subscription, "id", subscriptionId);
        return subscription;
    }

    private Subscription realPaidSubscription(Long subscriberId) {
        Subscription subscription = Subscription.createPaid(subscriberId, CREATOR_ID, 15000, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", PERIOD_END_AT);
        return subscription;
    }

    private Subscription paidSubscription(Long creatorId, SubscriptionStatus status) {
        Subscription subscription = mock(Subscription.class);
        when(subscription.getCreatorId()).thenReturn(creatorId);
        when(subscription.getSubscriptionLevel()).thenReturn(SubscriptionLevel.PAID);
        when(subscription.getStatus()).thenReturn(status);
        when(subscription.getStartedAt()).thenReturn(LATER_STARTED_AT);
        when(subscription.getCurrentPeriodEndAt()).thenReturn(PERIOD_END_AT);
        if (status == SubscriptionStatus.CANCEL_SCHEDULED) {
            lenient().when(subscription.getId()).thenReturn(20L);
            lenient().when(subscription.getCancelScheduledAt()).thenReturn(CANCEL_SCHEDULED_AT);
        }
        return subscription;
    }

    private Member member(Long id, Role role) {
        return member(id, role, "creator" + id, null);
    }

    private Member member(Long id, Role role, String nickname, String profileImageUrl) {
        Member member = Member.createLocalMember("member" + id + "@handdam.com", "password", nickname);
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "role", role);
        ReflectionTestUtils.setField(member, "profileImageUrl", profileImageUrl);
        return member;
    }
}
