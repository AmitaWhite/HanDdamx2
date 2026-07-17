package com.white.handdam.payment.service;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.payment.dto.request.PaymentConfirmRequest;
import com.white.handdam.payment.dto.request.PaymentFailRequest;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.response.PaymentFailResponse;
import com.white.handdam.payment.dto.toss.TossConfirmResponse;
import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.exception.PaymentErrorCode;
import com.white.handdam.payment.repository.PaymentRepository;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private static final String ORDER_ID_PREFIX = "HANDDAM-";
    private static final String UNKNOWN_FAILURE_CODE = "UNKNOWN_TOSS_CONFIRM_FAILURE";
    private static final int FAILURE_CODE_MAX_LENGTH = 100;
    private static final int FAILURE_MESSAGE_MAX_LENGTH = 500;

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public Payment createPendingPayment(Long memberId, Long creatorId, int amount) {
        Payment payment = Payment.prepare(
                memberId,
                creatorId,
                generateOrderId(),
                UUID.randomUUID().toString(),
                generateCustomerKey(),
                amount
        );

        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentConfirmStartResult startConfirm(
            Long memberId,
            PaymentConfirmRequest request
    ) {
        Payment payment = findPaymentForUpdate(request.orderId());
        validateOwner(payment, memberId);
        validateAmount(payment, request.amount());
        validateDuplicatePaymentKey(request.paymentKey(), payment.getId());

        if (payment.isPending()) {
            payment.startConfirm();
            return PaymentConfirmStartResult.requiresToss(toCommand(payment, request.paymentKey()));
        }

        if (payment.isConfirming()) {
            return PaymentConfirmStartResult.requiresToss(toCommand(payment, request.paymentKey()));
        }

        if (payment.isSuccess()) {
            validateSuccessfulPaymentKey(payment, request.paymentKey());
            return PaymentConfirmStartResult.alreadySucceeded(buildConfirmResponse(payment));
        }

        if (payment.isFailed()) {
            throw new CustomException(PaymentErrorCode.PAYMENT_ALREADY_FAILED);
        }

        throw new CustomException(PaymentErrorCode.PAYMENT_NOT_CONFIRMABLE);
    }

    @Transactional
    public PaymentConfirmResponse completeSuccess(
            PaymentConfirmCommand command,
            TossConfirmResponse tossResponse
    ) {
        Payment payment = findPaymentForUpdate(command.orderId());

        if (payment.isSuccess()) {
            return buildConfirmResponse(payment);
        }

        if (!payment.isConfirming()) {
            throw new CustomException(PaymentErrorCode.PAYMENT_CONFLICT);
        }

        validateDuplicatePaymentKey(command.paymentKey(), payment.getId());

        Subscription subscription = createOrUpgradePaidSubscription(
                payment.getMemberId(),
                payment.getCreatorId(),
                payment.getAmount(),
                tossResponse.approvedAt()
        );

        try {
            payment.completeSuccess(
                    command.paymentKey(),
                    tossResponse.method(),
                    tossResponse.approvedAt(),
                    subscription.getId()
            );
            paymentRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(PaymentErrorCode.DUPLICATE_PAYMENT_KEY);
        }

        return PaymentConfirmResponse.of(payment, subscription);
    }

    @Transactional
    public void failConfirming(String orderId, String failureCode, String failureMessage) {
        Payment payment = findPaymentForUpdate(orderId);
        if (payment.isConfirming()) {
            payment.failConfirming(
                    sanitizeFailureCode(failureCode),
                    sanitizeFailureMessage(failureMessage)
            );
        }
    }

    @Transactional
    public PaymentFailResponse fail(Long memberId, PaymentFailRequest request) {
        Payment payment = findPaymentForUpdate(request.orderId());
        validateOwner(payment, memberId);

        if (payment.isPending()) {
            payment.failPending(
                    sanitizeFailureCode(request.code()),
                    sanitizeFailureMessage(request.message())
            );
            return PaymentFailResponse.from(payment);
        }

        if (payment.isFailed()) {
            return PaymentFailResponse.from(payment);
        }

        if (payment.isConfirming() || payment.isSuccess()) {
            throw new CustomException(PaymentErrorCode.PAYMENT_CONFLICT);
        }

        throw new CustomException(PaymentErrorCode.PAYMENT_NOT_CONFIRMABLE);
    }

    private Subscription createOrUpgradePaidSubscription(
            Long memberId,
            Long creatorId,
            int amount,
            Instant approvedAt
    ) {
        return subscriptionRepository.findBySubscriberIdAndCreatorIdForUpdate(memberId, creatorId)
                .map(subscription -> upgradeExistingSubscription(subscription, amount, approvedAt))
                .orElseGet(() -> savePaidSubscription(memberId, creatorId, amount, approvedAt));
    }

    private Subscription upgradeExistingSubscription(
            Subscription subscription,
            int amount,
            Instant approvedAt
    ) {
        if (subscription.isPaid()) {
            throw new CustomException(PaymentErrorCode.PAID_SUBSCRIPTION_ALREADY_EXISTS);
        }

        subscription.upgradeToPaid(amount, approvedAt);
        return subscription;
    }

    private Subscription savePaidSubscription(
            Long memberId,
            Long creatorId,
            int amount,
            Instant approvedAt
    ) {
        try {
            Subscription subscription = subscriptionRepository.save(
                    Subscription.createPaid(memberId, creatorId, amount, approvedAt)
            );
            subscriptionRepository.flush();
            return subscription;
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(PaymentErrorCode.PAID_SUBSCRIPTION_ALREADY_EXISTS);
        } catch (CustomException exception) {
            if (exception.getErrorCode() == SubscriptionErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED) {
                throw exception;
            }
            throw exception;
        }
    }

    private PaymentConfirmResponse buildConfirmResponse(Payment payment) {
        Long subscriptionId = payment.getSubscriptionId();
        if (subscriptionId == null) {
            throw new CustomException(PaymentErrorCode.PAYMENT_CONFLICT);
        }

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_CONFLICT));

        return PaymentConfirmResponse.of(payment, subscription);
    }

    private PaymentConfirmCommand toCommand(Payment payment, String paymentKey) {
        return new PaymentConfirmCommand(
                payment.getId(),
                payment.getMemberId(),
                payment.getCreatorId(),
                paymentKey,
                payment.getOrderId(),
                payment.getAmount(),
                payment.getIdempotencyKey()
        );
    }

    private Payment findPaymentForUpdate(String orderId) {
        return paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validateOwner(Payment payment, Long memberId) {
        if (!Objects.equals(payment.getMemberId(), memberId)) {
            throw new CustomException(PaymentErrorCode.PAYMENT_OWNER_MISMATCH);
        }
    }

    private void validateAmount(Payment payment, Integer requestAmount) {
        if (requestAmount == null || payment.getAmount() != requestAmount) {
            throw new CustomException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void validateDuplicatePaymentKey(String paymentKey, Long paymentId) {
        if (paymentRepository.existsByPgTransactionIdAndIdNot(paymentKey, paymentId)) {
            throw new CustomException(PaymentErrorCode.DUPLICATE_PAYMENT_KEY);
        }
    }

    private void validateSuccessfulPaymentKey(Payment payment, String requestPaymentKey) {
        if (!Objects.equals(payment.getPgTransactionId(), requestPaymentKey)) {
            throw new CustomException(PaymentErrorCode.PAYMENT_NOT_CONFIRMABLE);
        }
    }

    private String sanitizeFailureCode(String failureCode) {
        String value = failureCode;
        if (value == null || value.isBlank()) {
            value = UNKNOWN_FAILURE_CODE;
        }

        return truncate(value.trim(), FAILURE_CODE_MAX_LENGTH);
    }

    private String sanitizeFailureMessage(String failureMessage) {
        if (failureMessage == null) {
            return null;
        }

        return truncate(failureMessage.trim(), FAILURE_MESSAGE_MAX_LENGTH);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String generateOrderId() {
        return ORDER_ID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateCustomerKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
