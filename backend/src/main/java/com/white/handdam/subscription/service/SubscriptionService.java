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
import com.white.handdam.subscription.dto.response.SubscriptionDetailResponse;
import com.white.handdam.subscription.dto.response.SubscriptionPlanResponse;
import com.white.handdam.subscription.dto.response.SubscriptionStatusResponse;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final CreatorProfileRepository creatorProfileRepository;

    @Transactional
    public FreeSubscriptionResponse createFreeSubscription(Long subscriberId, Long creatorId) {
        validateRequiredIds(subscriberId, creatorId);

        subscriptionRepository.findBySubscriberIdAndCreatorId(subscriberId, creatorId)
                .ifPresent(existingSubscription -> {
                    throw new CustomException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
                });

        Subscription subscription =
                Subscription.createFree(
                        subscriberId,
                        creatorId,
                        Instant.now()
                );

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        return FreeSubscriptionResponse.from(savedSubscription);
    }

    @Transactional
    public void cancelFreeSubscription(Long subscriberId, Long creatorId) {
        validateRequiredIds(subscriberId, creatorId);

        Subscription subscription = subscriptionRepository.findBySubscriberIdAndCreatorId(subscriberId, creatorId)
                .orElseThrow(() -> new CustomException(SubscriptionErrorCode.FREE_SUBSCRIPTION_NOT_FOUND));

        if (!subscription.isFree()) {
            throw new CustomException(SubscriptionErrorCode.PAID_SUBSCRIPTION_CANNOT_BE_CANCELED_AS_FREE);
        }

        subscriptionRepository.delete(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusResponse getSubscriptionStatus(Long subscriberId, Long creatorId) {
        validateRequiredIds(subscriberId, creatorId);
        validateCreator(creatorId);

        if (subscriberId.equals(creatorId)) {
            return SubscriptionStatusResponse.notSubscribed(creatorId);
        }

        return subscriptionRepository.findBySubscriberIdAndCreatorId(subscriberId, creatorId)
                .map(subscription ->
                        SubscriptionStatusResponse.subscribed(subscription)
                )
                .orElseGet(() -> SubscriptionStatusResponse.notSubscribed(creatorId));
    }

    @Transactional(readOnly = true)
    public List<MySubscriptionResponse> getMySubscriptions(Long subscriberId) {
        validateRequired(subscriberId, "subscriberId");

        List<Subscription> subscriptions =
                subscriptionRepository.findBySubscriberIdOrderByStartedAtDesc(subscriberId);

        if (subscriptions.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> creatorIds = subscriptions.stream()
                .map(subscription -> subscription.getCreatorId())
                .collect(Collectors.toCollection(
                        () -> new LinkedHashSet<>()
                ));
        Map<Long, Member> creatorsById = memberRepository.findAllById(creatorIds).stream()
                .collect(Collectors.toMap(
                        member -> member.getId(),
                        member -> member
                ));

        return subscriptions.stream()
                .map(subscription -> MySubscriptionResponse.from(
                        subscription,
                        findCreatorInMap(subscription.getCreatorId(), creatorsById)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionDetailResponse getSubscription(Long subscriberId, Long subscriptionId) {
        validateRequired(subscriberId, "subscriberId");
        validateRequired(subscriptionId, "subscriptionId");

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        validateOwner(subscription, subscriberId);

        Member creator = memberRepository.findById(subscription.getCreatorId())
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));

        return SubscriptionDetailResponse.from(subscription, creator);
    }

    @Transactional
    public SubscriptionDetailResponse scheduleCancellation(Long subscriberId, Long subscriptionId) {
        validateRequired(subscriberId, "subscriberId");
        validateRequired(subscriptionId, "subscriptionId");

        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        validateOwner(subscription, subscriberId);

        subscription.scheduleCancellation(Instant.now());

        Member creator = memberRepository.findById(subscription.getCreatorId())
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));

        return SubscriptionDetailResponse.from(subscription, creator);
    }

    @Transactional
    public SubscriptionDetailResponse revokeCancellationSchedule(Long subscriberId, Long subscriptionId) {
        validateRequired(subscriberId, "subscriberId");
        validateRequired(subscriptionId, "subscriptionId");

        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new CustomException(SubscriptionErrorCode.SUBSCRIPTION_NOT_FOUND));
        validateOwner(subscription, subscriberId);

        subscription.revokeCancellationSchedule();

        Member creator = memberRepository.findById(subscription.getCreatorId())
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));

        return SubscriptionDetailResponse.from(subscription, creator);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getSubscriptionPlans(Long creatorId) {
        validateRequired(creatorId, "creatorId");
        validateCreator(creatorId);

        CreatorProfile creatorProfile = creatorProfileRepository.findByMemberId(creatorId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        int paidPrice = creatorProfile.getSubscriptionPrice();
        boolean paidAvailable = paidPrice > 0;

        return List.of(
                SubscriptionPlanResponse.free(creatorId),
                SubscriptionPlanResponse.paid(
                        creatorId,
                        paidPrice,
                        paidAvailable,
                        creatorProfile.getBenefitsDescription()
                )
        );
    }

    private Member validateCreator(Long creatorId) {
        Member creator = memberRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));

        if (creator.getRole() != Role.CREATOR) {
            throw new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND);
        }

        return creator;
    }

    private void validateOwner(Subscription subscription, Long subscriberId) {
        if (!Objects.equals(subscription.getSubscriberId(), subscriberId)) {
            throw new CustomException(SubscriptionErrorCode.SUBSCRIPTION_OWNER_MISMATCH);
        }
    }

    private Member findCreatorInMap(Long creatorId, Map<Long, Member> creatorsById) {
        Member creator = creatorsById.get(creatorId);
        if (creator == null) {
            throw new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND);
        }
        return creator;
    }

    private void validateRequiredIds(Long subscriberId, Long creatorId) {
        validateRequired(subscriberId, "subscriberId");
        validateRequired(creatorId, "creatorId");
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
