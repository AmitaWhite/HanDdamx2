package com.white.handdam.subscription.service;

import com.white.handdam.notification.entity.NotificationEntity;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.repository.NotificationRepository;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.event.SubscriptionExpiringSoonEvent;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "jwt.secret=test_secret_for_subscription_expiring_soon_notification_1234567890",
        "subscription.expiration.enabled=false",
        "subscription.expiring-notification.enabled=false"
})
@ActiveProfiles("test")
class SubscriptionExpiringSoonNotificationIntegrationTest {

    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant TARGET_START = Instant.parse("2026-08-12T15:00:00Z");
    private static final Instant TARGET_END = Instant.parse("2026-08-13T15:00:00Z");
    private static final Instant PERIOD_END_AT = Instant.parse("2026-08-13T00:00:00Z");

    @Autowired
    private SubscriptionExpiringSoonService expiringSoonService;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RollbackSubscriptionEventPublisher rollbackEventPublisher;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private NotificationPublisher notificationPublisher;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        subscriptionRepository.deleteAll();
    }

    @Test
    @DisplayName("committed expiring soon event creates SUBSCRIPTION_EXPIRING notification")
    void committedExpiringSoonEventCreatesNotification() {
        Subscription subscription = saveScheduledPaid(SUBSCRIBER_ID, CREATOR_ID, PERIOD_END_AT);

        SubscriptionExpiringSoonResult result =
                expiringSoonService.publishExpiringSoonEvents(TARGET_START, TARGET_END, 100);

        assertThat(result.candidateCount()).isEqualTo(1);
        assertThat(result.publishedCount()).isEqualTo(1);
        List<NotificationEntity> notifications = waitForNotificationCount(1);
        NotificationEntity notification = notifications.get(0);
        assertThat(notification.getMemberId()).isEqualTo(SUBSCRIBER_ID);
        assertThat(notification.getSenderId()).isNull();
        assertThat(notification.getType()).isEqualTo(NotificationType.SUBSCRIPTION_EXPIRING);
        assertThat(notification.getReferenceId()).isEqualTo(subscription.getId());
        assertThat(notification.getReferenceType()).isEqualTo(NotificationReferenceType.SUBSCRIPTION.name());
    }

    @Test
    @DisplayName("subscriptions expiring before or after target date range do not create notifications")
    void outsideTargetDateRangeDoesNotCreateNotification() throws InterruptedException {
        saveScheduledPaid(SUBSCRIBER_ID, CREATOR_ID, TARGET_START.minusSeconds(1));
        saveScheduledPaid(SUBSCRIBER_ID + 1, CREATOR_ID + 1, TARGET_END);

        SubscriptionExpiringSoonResult result =
                expiringSoonService.publishExpiringSoonEvents(TARGET_START, TARGET_END, 100);

        assertThat(result.candidateCount()).isZero();
        Thread.sleep(300);
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("rolled back publishing transaction does not create notification")
    void rolledBackTransactionDoesNotCreateNotification() throws InterruptedException {
        assertThatThrownBy(() -> rollbackEventPublisher.publishAndRollback())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rollback");

        Thread.sleep(300);
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("active paid subscription does not create expiring soon notification")
    void activePaidSubscriptionDoesNotCreateNotification() throws InterruptedException {
        saveActivePaid(SUBSCRIBER_ID, CREATOR_ID, PERIOD_END_AT);

        SubscriptionExpiringSoonResult result =
                expiringSoonService.publishExpiringSoonEvents(TARGET_START, TARGET_END, 100);

        assertThat(result.candidateCount()).isZero();
        Thread.sleep(300);
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("already converted free subscription does not create expiring soon notification")
    void freeSubscriptionDoesNotCreateNotification() throws InterruptedException {
        subscriptionRepository.saveAndFlush(Subscription.createFree(SUBSCRIBER_ID, CREATOR_ID, STARTED_AT));

        SubscriptionExpiringSoonResult result =
                expiringSoonService.publishExpiringSoonEvents(TARGET_START, TARGET_END, 100);

        assertThat(result.candidateCount()).isZero();
        Thread.sleep(300);
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    private Subscription saveScheduledPaid(Long subscriberId, Long creatorId, Instant periodEndAt) {
        Subscription subscription = Subscription.createPaid(subscriberId, creatorId, 15000, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", periodEndAt);
        subscription.scheduleCancellation(STARTED_AT.plusSeconds(60));
        return subscriptionRepository.saveAndFlush(subscription);
    }

    private Subscription saveActivePaid(Long subscriberId, Long creatorId, Instant periodEndAt) {
        Subscription subscription = Subscription.createPaid(subscriberId, creatorId, 15000, STARTED_AT);
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", periodEndAt);
        return subscriptionRepository.saveAndFlush(subscription);
    }

    private List<NotificationEntity> waitForNotificationCount(int expectedCount) {
        for (int attempt = 0; attempt < 20; attempt++) {
            List<NotificationEntity> notifications = notificationRepository.findAll();
            if (notifications.size() == expectedCount) {
                return notifications;
            }
            sleep();
        }
        return notificationRepository.findAll();
    }

    private void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @TestConfiguration
    static class RollbackSubscriptionEventPublisherConfig {

        @Bean
        RollbackSubscriptionEventPublisher rollbackSubscriptionEventPublisher(
                ApplicationEventPublisher eventPublisher
        ) {
            return new RollbackSubscriptionEventPublisher(eventPublisher);
        }
    }

    static class RollbackSubscriptionEventPublisher {

        private final ApplicationEventPublisher eventPublisher;

        RollbackSubscriptionEventPublisher(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        void publishAndRollback() {
            eventPublisher.publishEvent(new SubscriptionExpiringSoonEvent(
                    999L,
                    SUBSCRIBER_ID,
                    CREATOR_ID,
                    PERIOD_END_AT
            ));
            throw new IllegalStateException("rollback");
        }
    }
}
