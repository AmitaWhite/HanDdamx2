package com.white.handdam.subscription.service;

import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionExpirationService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionExpirationTransactionService transactionService;

    @Transactional(readOnly = true)
    public List<Long> findExpiredSubscriptionIds(Instant now, int batchSize) {
        validateRequired(now, "now");
        validateBatchSize(batchSize);

        return subscriptionRepository.findExpiredSubscriptionIds(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                now,
                PageRequest.of(0, batchSize)
        );
    }

    public SubscriptionExpirationResult expireScheduledSubscriptions(Instant now, int batchSize) {
        List<Long> subscriptionIds = findExpiredSubscriptionIds(now, batchSize);

        int convertedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Long subscriptionId : subscriptionIds) {
            try {
                boolean converted = transactionService.convertExpiredSubscriptionToFree(subscriptionId, now);
                if (converted) {
                    convertedCount++;
                } else {
                    skippedCount++;
                }
            } catch (Exception exception) {
                failedCount++;
                log.warn("Failed to expire subscription. subscriptionId={}", subscriptionId, exception);
            }
        }

        return new SubscriptionExpirationResult(
                subscriptionIds.size(),
                convertedCount,
                skippedCount,
                failedCount
        );
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private void validateBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
    }
}
