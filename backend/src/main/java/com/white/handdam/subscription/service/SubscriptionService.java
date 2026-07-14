package com.white.handdam.subscription.service;

import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.subscription.dto.response.FreeSubscriptionResponse;
import com.white.handdam.subscription.dto.response.MySubscriptionResponse;
import com.white.handdam.subscription.dto.response.SubscriptionPlanResponse;
import com.white.handdam.subscription.dto.response.SubscriptionStatusResponse;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import com.white.handdam.subscription.repository.SubscriptionPlanQueryRepository;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import com.white.handdam.subscription.repository.projection.CreatorSubscriptionPlanProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private static final String CREATOR_NOT_FOUND_MESSAGE = "크리에이터를 찾을 수 없습니다.";
    private static final String SUBSCRIPTION_PLAN_NOT_FOUND_MESSAGE = "구독 플랜 정보를 찾을 수 없습니다.";

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionPlanQueryRepository subscriptionPlanQueryRepository;

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
    public List<SubscriptionPlanResponse> getSubscriptionPlans(Long creatorId) {
        validateRequired(creatorId, "creatorId");
        validateCreator(creatorId);

        CreatorSubscriptionPlanProjection projection =
                subscriptionPlanQueryRepository.findByCreatorId(creatorId)
                        .orElseThrow(() -> new CustomException(
                                CommonErrorCode.RESOURCE_NOT_FOUND,
                                SUBSCRIPTION_PLAN_NOT_FOUND_MESSAGE
                        ));

        Integer paidPrice = projection.subscriptionPrice();
        boolean paidAvailable = paidPrice != null && paidPrice > 0;

        return List.of(
                SubscriptionPlanResponse.free(creatorId),
                SubscriptionPlanResponse.paid(
                        creatorId,
                        paidPrice,
                        paidAvailable,
                        projection.benefitsDescription()
                )
        );
    }

    private Member validateCreator(Long creatorId) {
        Member creator = memberRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        CREATOR_NOT_FOUND_MESSAGE
                ));

        if (creator.getRole() != Role.CREATOR) {
            throw new CustomException(CommonErrorCode.RESOURCE_NOT_FOUND, CREATOR_NOT_FOUND_MESSAGE);
        }

        return creator;
    }

    private Member findCreatorInMap(Long creatorId, Map<Long, Member> creatorsById) {
        Member creator = creatorsById.get(creatorId);
        if (creator == null) {
            throw new CustomException(CommonErrorCode.RESOURCE_NOT_FOUND, CREATOR_NOT_FOUND_MESSAGE);
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
