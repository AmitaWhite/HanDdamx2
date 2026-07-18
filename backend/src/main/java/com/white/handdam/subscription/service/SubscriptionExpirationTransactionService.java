package com.white.handdam.subscription.service;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SubscriptionExpirationTransactionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public boolean convertExpiredSubscriptionToFree(Long subscriptionId, Instant now) {
        validateRequired(subscriptionId, "subscriptionId");
        validateRequired(now, "now");

        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElse(null);
        if (subscription == null) {
            return false;
        }

        if (!subscription.isExpiredCancelScheduledPaid(now)) {
            return false;
        }

        subscription.convertToFreeAfterPeriodEnd(now);
        return true;
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
