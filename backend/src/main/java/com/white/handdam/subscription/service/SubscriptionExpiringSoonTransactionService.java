package com.white.handdam.subscription.service;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.event.SubscriptionExpiringSoonEvent;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SubscriptionExpiringSoonTransactionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public boolean publishExpiringSoonEvent(
            Long subscriptionId,
            Instant expectedCurrentPeriodEndAt,
            Instant now,
            Instant threshold
    ) {
        validateRequired(subscriptionId, "subscriptionId");
        validateRequired(expectedCurrentPeriodEndAt, "expectedCurrentPeriodEndAt");
        validateTimeRange(now, threshold);

        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElse(null);
        if (subscription == null) {
            return false;
        }

        if (!isTarget(subscription, expectedCurrentPeriodEndAt, now, threshold)) {
            return false;
        }

        eventPublisher.publishEvent(new SubscriptionExpiringSoonEvent(
                subscription.getId(),
                subscription.getSubscriberId(),
                subscription.getCreatorId(),
                subscription.getCurrentPeriodEndAt()
        ));
        return true;
    }

    private boolean isTarget(
            Subscription subscription,
            Instant expectedCurrentPeriodEndAt,
            Instant now,
            Instant threshold
    ) {
        Instant currentPeriodEndAt = subscription.getCurrentPeriodEndAt();
        return subscription.getSubscriptionLevel() == SubscriptionLevel.PAID
                && subscription.getStatus() == SubscriptionStatus.CANCEL_SCHEDULED
                && currentPeriodEndAt != null
                && Objects.equals(currentPeriodEndAt, expectedCurrentPeriodEndAt)
                && currentPeriodEndAt.isAfter(now)
                && !currentPeriodEndAt.isAfter(threshold);
    }

    private void validateTimeRange(Instant now, Instant threshold) {
        validateRequired(now, "now");
        validateRequired(threshold, "threshold");
        if (threshold.isBefore(now)) {
            throw new IllegalArgumentException("threshold must not be before now");
        }
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
