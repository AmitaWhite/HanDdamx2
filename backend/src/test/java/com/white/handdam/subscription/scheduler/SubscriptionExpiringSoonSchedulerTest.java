package com.white.handdam.subscription.scheduler;

import com.white.handdam.subscription.config.SubscriptionExpiringNotificationProperties;
import com.white.handdam.subscription.service.SubscriptionExpiringSoonResult;
import com.white.handdam.subscription.service.SubscriptionExpiringSoonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionExpiringSoonSchedulerTest {

    @Test
    @DisplayName("publishExpiringSoonEvents delegates to service with days-before threshold and batch size")
    void publishExpiringSoonEventsDelegatesToService() {
        SubscriptionExpiringSoonService expiringSoonService = mock(SubscriptionExpiringSoonService.class);
        SubscriptionExpiringNotificationProperties properties = new SubscriptionExpiringNotificationProperties();
        properties.setDaysBefore(3);
        properties.setBatchSize(50);
        SubscriptionExpiringSoonScheduler scheduler =
                new SubscriptionExpiringSoonScheduler(expiringSoonService, properties);
        when(expiringSoonService.publishExpiringSoonEvents(
                any(Instant.class),
                any(Instant.class),
                org.mockito.Mockito.eq(50)
        )).thenReturn(new SubscriptionExpiringSoonResult(2, 1, 1, 0));

        Instant before = Instant.now();
        scheduler.publishExpiringSoonEvents();
        Instant after = Instant.now();

        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(expiringSoonService).publishExpiringSoonEvents(
                nowCaptor.capture(),
                thresholdCaptor.capture(),
                org.mockito.Mockito.eq(50)
        );
        assertThat(nowCaptor.getValue()).isBetween(before, after);
        Duration thresholdOffset = Duration.between(nowCaptor.getValue(), thresholdCaptor.getValue());
        assertThat(thresholdOffset).isEqualTo(Duration.ofDays(3));
    }

    @Test
    @DisplayName("publishExpiringSoonEvents uses configured cron and timezone placeholders")
    void publishExpiringSoonEventsUsesCronAndTimezone() throws NoSuchMethodException {
        Method method = SubscriptionExpiringSoonScheduler.class.getMethod("publishExpiringSoonEvents");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${subscription.expiring-notification.cron}");
        assertThat(scheduled.zone()).isEqualTo("${subscription.expiring-notification.zone}");
    }
}
