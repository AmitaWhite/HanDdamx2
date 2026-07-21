package com.white.handdam.subscription.service;

import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionExpiringSoonTarget;
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
public class SubscriptionExpiringSoonService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionExpiringSoonTransactionService transactionService;

    @Transactional(readOnly = true)
    public List<SubscriptionExpiringSoonTarget> findExpiringSoonTargets(
            Instant now,
            Instant threshold,
            int batchSize,
            int pageNumber
    ) {
        validateTimeRange(now, threshold);
        validateBatchSize(batchSize);
        validatePageNumber(pageNumber);

        return subscriptionRepository.findExpiringSoonSubscriptionTargets(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                now,
                threshold,
                PageRequest.of(pageNumber, batchSize)
        );
    }

    public SubscriptionExpiringSoonResult publishExpiringSoonEvents(
            Instant now,
            Instant threshold,
            int batchSize
    ) {
        validateTimeRange(now, threshold);
        validateBatchSize(batchSize);

        int candidateCount = 0;
        int publishedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        int pageNumber = 0;

        while (true) {
            List<SubscriptionExpiringSoonTarget> targets =
                    findExpiringSoonTargets(now, threshold, batchSize, pageNumber);
            candidateCount += targets.size();

            for (SubscriptionExpiringSoonTarget target : targets) {
                try {
                    boolean published = transactionService.publishExpiringSoonEvent(
                            target.subscriptionId(),
                            target.currentPeriodEndAt(),
                            now,
                            threshold
                    );
                    if (published) {
                        publishedCount++;
                    } else {
                        skippedCount++;
                    }
                } catch (Exception exception) {
                    failedCount++;
                    log.warn("Failed to publish subscription expiring soon event. subscriptionId={}",
                            target.subscriptionId(), exception);
                }
            }

            if (targets.size() < batchSize) {
                break;
            }
            pageNumber++;
        }

        return new SubscriptionExpiringSoonResult(
                candidateCount,
                publishedCount,
                skippedCount,
                failedCount
        );
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

    private void validateBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
    }

    private void validatePageNumber(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
    }
}
