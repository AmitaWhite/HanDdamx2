package com.white.handdam.subscription.scheduler;

import com.white.handdam.subscription.config.SubscriptionExpirationProperties;
import com.white.handdam.subscription.service.SubscriptionExpirationResult;
import com.white.handdam.subscription.service.SubscriptionExpirationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "subscription.expiration",
        name = "enabled",
        havingValue = "true"
)
public class SubscriptionExpirationScheduler {

    private final SubscriptionExpirationService subscriptionExpirationService;
    private final SubscriptionExpirationProperties properties;

    @Scheduled(cron = "${subscription.expiration.cron}")
    public void expireScheduledSubscriptions() {
        Instant now = Instant.now();
        SubscriptionExpirationResult result =
                subscriptionExpirationService.expireScheduledSubscriptions(now, properties.getBatchSize());

        log.info(
                "Subscription expiration completed. candidateCount={}, convertedCount={}, skippedCount={}, failedCount={}",
                result.candidateCount(),
                result.convertedCount(),
                result.skippedCount(),
                result.failedCount()
        );
    }
}
