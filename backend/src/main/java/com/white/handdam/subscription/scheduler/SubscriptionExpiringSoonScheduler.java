package com.white.handdam.subscription.scheduler;

import com.white.handdam.subscription.config.SubscriptionExpiringNotificationProperties;
import com.white.handdam.subscription.service.SubscriptionExpiringSoonResult;
import com.white.handdam.subscription.service.SubscriptionExpiringSoonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "subscription.expiring-notification",
        name = "enabled",
        havingValue = "true"
)
public class SubscriptionExpiringSoonScheduler {

    private final SubscriptionExpiringSoonService subscriptionExpiringSoonService;
    private final SubscriptionExpiringNotificationProperties properties;

    @Scheduled(
            cron = "${subscription.expiring-notification.cron}",
            zone = "${subscription.expiring-notification.zone}"
    )
    public void publishExpiringSoonEvents() {
        ZoneId zoneId = ZoneId.of(properties.getZone());
        LocalDate targetDate = LocalDate.now(zoneId)
                .plusDays(properties.getDaysBefore());
        Instant targetStart = targetDate
                .atStartOfDay(zoneId)
                .toInstant();
        Instant targetEnd = targetDate
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant();
        SubscriptionExpiringSoonResult result =
                subscriptionExpiringSoonService.publishExpiringSoonEvents(
                        targetStart,
                        targetEnd,
                        properties.getBatchSize()
                );

        log.info(
                "Subscription expiring soon notification completed. candidateCount={}, publishedCount={}, skippedCount={}, failedCount={}",
                result.candidateCount(),
                result.publishedCount(),
                result.skippedCount(),
                result.failedCount()
        );
    }
}
