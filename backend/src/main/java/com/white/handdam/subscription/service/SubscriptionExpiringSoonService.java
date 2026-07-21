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
            Instant targetStart,
            Instant targetEnd,
            int batchSize,
            int pageNumber
    ) {
        validateTargetRange(targetStart, targetEnd);
        validateBatchSize(batchSize);
        validatePageNumber(pageNumber);

        return subscriptionRepository.findExpiringSoonSubscriptionTargets(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                targetStart,
                targetEnd,
                PageRequest.of(pageNumber, batchSize)
        );
    }

    public SubscriptionExpiringSoonResult publishExpiringSoonEvents(
            Instant targetStart,
            Instant targetEnd,
            int batchSize
    ) {
        validateTargetRange(targetStart, targetEnd);
        validateBatchSize(batchSize);

        int candidateCount = 0;
        int publishedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        int pageNumber = 0;

        while (true) {
            List<SubscriptionExpiringSoonTarget> targets =
                    findExpiringSoonTargets(targetStart, targetEnd, batchSize, pageNumber);
            candidateCount += targets.size();

            for (SubscriptionExpiringSoonTarget target : targets) {
                try {
                    boolean published = transactionService.publishExpiringSoonEvent(
                            target.subscriptionId(),
                            target.currentPeriodEndAt(),
                            targetStart,
                            targetEnd
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
