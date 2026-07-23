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
            Instant targetStart,
            Instant targetEnd
    ) {
        validateRequired(subscriptionId, "subscriptionId");
        validateRequired(expectedCurrentPeriodEndAt, "expectedCurrentPeriodEndAt");
        validateTargetRange(targetStart, targetEnd);

        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElse(null);
        if (subscription == null) {
            return false;
        }

        if (!isTarget(subscription, expectedCurrentPeriodEndAt, targetStart, targetEnd)) {
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
            Instant targetStart,
            Instant targetEnd
    ) {
        Instant currentPeriodEndAt = subscription.getCurrentPeriodEndAt();
        return subscription.getSubscriptionLevel() == SubscriptionLevel.PAID
                && subscription.getStatus() == SubscriptionStatus.CANCEL_SCHEDULED
                && currentPeriodEndAt != null
                && Objects.equals(currentPeriodEndAt, expectedCurrentPeriodEndAt)
                && !currentPeriodEndAt.isBefore(targetStart)
                && currentPeriodEndAt.isBefore(targetEnd);
    }

    private void validateTargetRange(Instant targetStart, Instant targetEnd) {
        validateRequired(targetStart, "targetStart");
        validateRequired(targetEnd, "targetEnd");
        if (!targetEnd.isAfter(targetStart)) {
            throw new IllegalArgumentException("targetEnd must be after targetStart");
        }
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
