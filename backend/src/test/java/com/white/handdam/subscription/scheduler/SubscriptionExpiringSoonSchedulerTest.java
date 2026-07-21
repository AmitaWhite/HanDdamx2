package com.white.handdam.subscription.scheduler;

import com.white.handdam.subscription.config.SubscriptionExpiringNotificationProperties;
import com.white.handdam.subscription.service.SubscriptionExpiringSoonResult;
import com.white.handdam.subscription.service.SubscriptionExpiringSoonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubscriptionExpiringSoonSchedulerTest {

    @Test
    @DisplayName("publishExpiringSoonEvents delegates to service with target date range and batch size")
    void publishExpiringSoonEventsDelegatesToService() {
        SubscriptionExpiringSoonService expiringSoonService = mock(SubscriptionExpiringSoonService.class);
        SubscriptionExpiringNotificationProperties properties = new SubscriptionExpiringNotificationProperties();
        properties.setDaysBefore(3);
        properties.setBatchSize(50);
        properties.setZone("Asia/Seoul");
        SubscriptionExpiringSoonScheduler scheduler =
                new SubscriptionExpiringSoonScheduler(expiringSoonService, properties);
        when(expiringSoonService.publishExpiringSoonEvents(
                any(Instant.class),
                any(Instant.class),
                org.mockito.Mockito.eq(50)
        )).thenReturn(new SubscriptionExpiringSoonResult(2, 1, 1, 0));

        scheduler.publishExpiringSoonEvents();

        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate targetDate = LocalDate.now(zoneId).plusDays(3);
        Instant expectedTargetStart = targetDate.atStartOfDay(zoneId).toInstant();
        Instant expectedTargetEnd = targetDate.plusDays(1).atStartOfDay(zoneId).toInstant();
        ArgumentCaptor<Instant> targetStartCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> targetEndCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(expiringSoonService).publishExpiringSoonEvents(
                targetStartCaptor.capture(),
                targetEndCaptor.capture(),
                org.mockito.Mockito.eq(50)
        );
        assertThat(targetStartCaptor.getValue()).isEqualTo(expectedTargetStart);
        assertThat(targetEndCaptor.getValue()).isEqualTo(expectedTargetEnd);
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
