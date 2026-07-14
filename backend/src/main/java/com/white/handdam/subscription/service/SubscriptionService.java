package com.white.handdam.subscription.service;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.subscription.dto.response.FreeSubscriptionResponse;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public FreeSubscriptionResponse createFreeSubscription(Long subscriberId, Long creatorId) {
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

    private void validateRequiredIds(Long subscriberId, Long creatorId) {
        if (subscriberId == null) {
            throw new IllegalArgumentException("subscriberId must not be null");
        }
        if (creatorId == null) {
            throw new IllegalArgumentException("creatorId must not be null");
        }
    }
}
