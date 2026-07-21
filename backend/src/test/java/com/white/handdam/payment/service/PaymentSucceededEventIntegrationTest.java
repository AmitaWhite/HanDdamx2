package com.white.handdam.payment.service;

import com.white.handdam.notification.entity.NotificationEntity;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.repository.NotificationRepository;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.toss.TossConfirmResponse;
import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.entity.PaymentStatus;
import com.white.handdam.payment.event.PaymentSucceededEvent;
import com.white.handdam.payment.repository.PaymentRepository;
import com.white.handdam.subscription.entity.SubscriptionLevel;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "jwt.secret=test_secret_for_payment_succeeded_event_integration_1234567890",
        "subscription.expiration.enabled=false"
})
@ActiveProfiles("test")
class PaymentSucceededEventIntegrationTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final String ORDER_ID = "HANDDAM-event-success";
    private static final String PAYMENT_KEY = "payment-key-event-success";
    private static final String IDEMPOTENCY_KEY = "idempotency-key-event-success";
    private static final String CUSTOMER_KEY = "customer-key-event-success";
    private static final int AMOUNT = 15000;
    private static final Instant APPROVED_AT = Instant.parse("2026-07-15T01:00:00Z");

    @Autowired
    private PaymentTransactionService paymentTransactionService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RollbackEventPublisher rollbackEventPublisher;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private NotificationPublisher notificationPublisher;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        paymentRepository.deleteAll();
        subscriptionRepository.deleteAll();
    }

    @Test
    @DisplayName("committed payment success event is handled after commit and creates payment notification")
    void committedPaymentSuccessEventCreatesNotification() {
        Payment payment = saveConfirmingPayment();

        PaymentConfirmResponse response =
                paymentTransactionService.completeSuccess(command(payment), tossResponse());

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.subscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);

        List<NotificationEntity> notifications = waitForNotificationCount(1);
        NotificationEntity notification = notifications.get(0);
        assertThat(notification.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(notification.getSenderId()).isNull();
        assertThat(notification.getType()).isEqualTo(NotificationType.PAYMENT_SUCCESS);
        assertThat(notification.getReferenceId()).isEqualTo(payment.getId());
        assertThat(notification.getReferenceType()).isEqualTo(NotificationReferenceType.PAYMENT.name());
    }

    @Test
    @DisplayName("duplicate confirm for already SUCCESS payment does not create duplicate notification")
    void duplicateConfirmDoesNotCreateDuplicateNotification() throws InterruptedException {
        Payment payment = saveConfirmingPayment();
        paymentTransactionService.completeSuccess(command(payment), tossResponse());
        waitForNotificationCount(1);

        PaymentConfirmResponse response =
                paymentTransactionService.completeSuccess(command(payment), tossResponse());

        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        Thread.sleep(300);
        assertThat(notificationRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("after commit listener does not create notification when publishing transaction rolls back")
    void rolledBackTransactionDoesNotCreateNotification() throws InterruptedException {
        assertThatThrownBy(() -> rollbackEventPublisher.publishAndRollback())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rollback");

        Thread.sleep(300);
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    private Payment saveConfirmingPayment() {
        Payment payment = Payment.prepare(
                MEMBER_ID,
                CREATOR_ID,
                ORDER_ID,
                IDEMPOTENCY_KEY,
                CUSTOMER_KEY,
                AMOUNT
        );
        payment.startConfirm();
        return paymentRepository.saveAndFlush(payment);
    }

    private PaymentConfirmCommand command(Payment payment) {
        return new PaymentConfirmCommand(
                payment.getId(),
                MEMBER_ID,
                CREATOR_ID,
                PAYMENT_KEY,
                ORDER_ID,
                AMOUNT,
                IDEMPOTENCY_KEY
        );
    }

    private TossConfirmResponse tossResponse() {
        return new TossConfirmResponse(PAYMENT_KEY, ORDER_ID, "DONE", AMOUNT, "CARD", APPROVED_AT);
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
    static class RollbackEventPublisherConfig {

        @Bean
        RollbackEventPublisher rollbackEventPublisher(ApplicationEventPublisher eventPublisher) {
            return new RollbackEventPublisher(eventPublisher);
        }
    }

    static class RollbackEventPublisher {

        private final ApplicationEventPublisher eventPublisher;

        RollbackEventPublisher(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        void publishAndRollback() {
            eventPublisher.publishEvent(new PaymentSucceededEvent(
                    999L,
                    MEMBER_ID,
                    AMOUNT,
                    "HANDDAM-rollback"
            ));
            throw new IllegalStateException("rollback");
        }
    }
}
