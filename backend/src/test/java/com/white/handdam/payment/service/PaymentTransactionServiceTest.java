package com.white.handdam.payment.service;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.payment.dto.request.PaymentConfirmRequest;
import com.white.handdam.payment.dto.request.PaymentFailRequest;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.response.PaymentFailResponse;
import com.white.handdam.payment.dto.toss.TossConfirmResponse;
import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.entity.PaymentStatus;
import com.white.handdam.payment.exception.PaymentErrorCode;
import com.white.handdam.payment.repository.PaymentRepository;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 9L;
    private static final Long CREATOR_ID = 2L;
    private static final Long PAYMENT_ID = 10L;
    private static final Long SUBSCRIPTION_ID = 20L;
    private static final String ORDER_ID = "HANDDAM-1234567890abcdef";
    private static final String PAYMENT_KEY = "payment-key";
    private static final String IDEMPOTENCY_KEY = "idempotency-key";
    private static final Instant APPROVED_AT = Instant.parse("2026-07-15T01:00:00Z");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private PaymentTransactionService paymentTransactionService;

    @Test
    @DisplayName("startConfirm locks a PENDING payment and moves it to CONFIRMING")
    void startConfirmMovesPendingToConfirming() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        PaymentConfirmStartResult result =
                paymentTransactionService.startConfirm(MEMBER_ID, confirmRequest());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMING);
        assertThat(result.alreadySucceeded()).isFalse();
        assertThat(result.command().idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        verify(paymentRepository).findByOrderIdForUpdate(ORDER_ID);
    }

    @Test
    @DisplayName("startConfirm propagates payment lock acquisition failure")
    void startConfirmPropagatesPaymentLockAcquisitionFailure() {
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> paymentTransactionService.startConfirm(MEMBER_ID, confirmRequest()))
                .isInstanceOf(CannotAcquireLockException.class);

        verify(paymentRepository).findByOrderIdForUpdate(ORDER_ID);
    }

    @Test
    @DisplayName("startConfirm allows CONFIRMING retry with the same DB idempotency key")
    void startConfirmAllowsConfirmingRetry() {
        Payment payment = confirmingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        PaymentConfirmStartResult result =
                paymentTransactionService.startConfirm(MEMBER_ID, confirmRequest());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMING);
        assertThat(result.command().idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    @DisplayName("startConfirm returns existing success response without state change")
    void startConfirmReturnsExistingSuccessResponse() {
        Payment payment = successPayment(SUBSCRIPTION_ID);
        Subscription subscription = paidSubscription(SUBSCRIPTION_ID);
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        PaymentConfirmStartResult result =
                paymentTransactionService.startConfirm(MEMBER_ID, confirmRequest());

        assertThat(result.alreadySucceeded()).isTrue();
        assertThat(result.existingResponse().status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.existingResponse().subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("startConfirm rejects FAILED payment")
    void startConfirmRejectsFailedPayment() {
        Payment payment = pendingPayment();
        payment.failPending("FAIL", "failed");
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentTransactionService.startConfirm(MEMBER_ID, confirmRequest()))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAYMENT_ALREADY_FAILED)
                );
    }

    @Test
    @DisplayName("startConfirm rejects owner mismatch")
    void startConfirmRejectsOwnerMismatch() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentTransactionService.startConfirm(OTHER_MEMBER_ID, confirmRequest()))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAYMENT_OWNER_MISMATCH)
                );
    }

    @Test
    @DisplayName("startConfirm rejects amount mismatch before Toss call")
    void startConfirmRejectsAmountMismatch() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 14000);

        assertThatThrownBy(() -> paymentTransactionService.startConfirm(MEMBER_ID, request))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH)
                );
    }

    @Test
    @DisplayName("completeSuccess creates a new PAID subscription when none exists")
    void completeSuccessCreatesPaidSubscription() {
        Payment payment = confirmingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findBySubscriberIdAndCreatorIdForUpdate(MEMBER_ID, CREATOR_ID))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> {
                    Subscription subscription = invocation.getArgument(0);
                    ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
                    return subscription;
                });

        PaymentConfirmResponse response =
                paymentTransactionService.completeSuccess(command(), tossResponse());

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        Subscription subscription = subscriptionCaptor.getValue();

        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCurrentPeriodStartAt()).isEqualTo(APPROVED_AT);
        assertThat(subscription.getCurrentPeriodEndAt()).isEqualTo(
                APPROVED_AT.atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant()
        );
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getPgTransactionId()).isEqualTo(PAYMENT_KEY);
        assertThat(payment.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(response.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("completeSuccess does not complete payment when subscription lock acquisition fails")
    void completeSuccessDoesNotCompletePaymentWhenSubscriptionLockAcquisitionFails() {
        Payment payment = confirmingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findBySubscriberIdAndCreatorIdForUpdate(MEMBER_ID, CREATOR_ID))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> paymentTransactionService.completeSuccess(command(), tossResponse()))
                .isInstanceOf(CannotAcquireLockException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CONFIRMING);
        assertThat(payment.getSubscriptionId()).isNull();
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("completeSuccess upgrades an existing FREE subscription without creating a new row")
    void completeSuccessUpgradesFreeSubscription() {
        Payment payment = confirmingPayment();
        Subscription subscription = Subscription.createFree(MEMBER_ID, CREATOR_ID, APPROVED_AT);
        ReflectionTestUtils.setField(subscription, "id", SUBSCRIPTION_ID);
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findBySubscriberIdAndCreatorIdForUpdate(MEMBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(subscription));

        PaymentConfirmResponse response =
                paymentTransactionService.completeSuccess(command(), tossResponse());

        assertThat(subscription.getId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(subscription.getSubscriptionLevel()).isEqualTo(SubscriptionLevel.PAID);
        assertThat(subscription.getSubscriptionPriceSnapshot()).isEqualTo(15000);
        assertThat(payment.getSubscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(response.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    @DisplayName("completeSuccess blocks an existing PAID subscription")
    void completeSuccessBlocksExistingPaidSubscription() {
        Payment payment = confirmingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        when(subscriptionRepository.findBySubscriberIdAndCreatorIdForUpdate(MEMBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(paidSubscription(SUBSCRIPTION_ID)));

        assertThatThrownBy(() -> paymentTransactionService.completeSuccess(command(), tossResponse()))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAID_SUBSCRIPTION_ALREADY_EXISTS)
                );
    }

    @Test
    @DisplayName("fail changes PENDING payment to FAILED")
    void failChangesPendingToFailed() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        PaymentFailResponse response = paymentTransactionService.fail(
                MEMBER_ID,
                new PaymentFailRequest(ORDER_ID, "PAY_PROCESS_CANCELED", "canceled")
        );

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureCode()).isEqualTo("PAY_PROCESS_CANCELED");
        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("fail returns existing response for FAILED payment")
    void failReturnsExistingResponseForFailedPayment() {
        Payment payment = pendingPayment();
        payment.failPending("PAY_PROCESS_CANCELED", "canceled");
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        PaymentFailResponse response = paymentTransactionService.fail(
                MEMBER_ID,
                new PaymentFailRequest(ORDER_ID, "PAY_PROCESS_CANCELED", "canceled")
        );

        assertThat(response.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.failureCode()).isEqualTo("PAY_PROCESS_CANCELED");
    }

    @Test
    @DisplayName("fail rejects CONFIRMING payment")
    void failRejectsConfirmingPayment() {
        Payment payment = confirmingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentTransactionService.fail(
                MEMBER_ID,
                new PaymentFailRequest(ORDER_ID, "PAY_PROCESS_CANCELED", "canceled")
        ))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAYMENT_CONFLICT)
                );
    }

    @Test
    @DisplayName("fail truncates long failure message")
    void failTruncatesLongFailureMessage() {
        Payment payment = pendingPayment();
        when(paymentRepository.findByOrderIdForUpdate(ORDER_ID)).thenReturn(Optional.of(payment));
        String longMessage = "a".repeat(600);

        paymentTransactionService.fail(
                MEMBER_ID,
                new PaymentFailRequest(ORDER_ID, "FAIL", longMessage)
        );

        assertThat(payment.getFailureMessage()).hasSize(500);
    }

    private PaymentConfirmRequest confirmRequest() {
        return new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
    }

    private PaymentConfirmCommand command() {
        return new PaymentConfirmCommand(
                PAYMENT_ID,
                MEMBER_ID,
                CREATOR_ID,
                PAYMENT_KEY,
                ORDER_ID,
                15000,
                IDEMPOTENCY_KEY
        );
    }

    private TossConfirmResponse tossResponse() {
        return new TossConfirmResponse(PAYMENT_KEY, ORDER_ID, "DONE", 15000, "카드", APPROVED_AT);
    }

    private Payment pendingPayment() {
        Payment payment = Payment.prepare(
                MEMBER_ID,
                CREATOR_ID,
                ORDER_ID,
                IDEMPOTENCY_KEY,
                "customer-key",
                15000
        );
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        return payment;
    }

    private Payment confirmingPayment() {
        Payment payment = pendingPayment();
        payment.startConfirm();
        return payment;
    }

    private Payment successPayment(Long subscriptionId) {
        Payment payment = confirmingPayment();
        payment.completeSuccess(PAYMENT_KEY, "카드", APPROVED_AT, subscriptionId);
        return payment;
    }

    private Subscription paidSubscription(Long subscriptionId) {
        Subscription subscription = Subscription.createPaid(MEMBER_ID, CREATOR_ID, 15000, APPROVED_AT);
        ReflectionTestUtils.setField(subscription, "id", subscriptionId);
        return subscription;
    }
}
