package com.white.handdam.subscription.scheduler;

import com.white.handdam.subscription.config.SubscriptionExpirationProperties;
import com.white.handdam.subscription.service.SubscriptionExpirationResult;
import com.white.handdam.subscription.service.SubscriptionExpirationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionExpirationSchedulerTest {

    @Test
    @DisplayName("expireScheduledSubscriptions delegates expiration processing to service")
    void expireScheduledSubscriptionsDelegatesToService() {
        SubscriptionExpirationService expirationService = mock(SubscriptionExpirationService.class);
        SubscriptionExpirationProperties properties = new SubscriptionExpirationProperties();
        properties.setBatchSize(50);
        SubscriptionExpirationScheduler scheduler =
                new SubscriptionExpirationScheduler(expirationService, properties);
        when(expirationService.expireScheduledSubscriptions(any(Instant.class), org.mockito.Mockito.eq(50)))
                .thenReturn(new SubscriptionExpirationResult(2, 1, 1, 0));

        scheduler.expireScheduledSubscriptions();

        verify(expirationService).expireScheduledSubscriptions(any(Instant.class), org.mockito.Mockito.eq(50));
    }
}
