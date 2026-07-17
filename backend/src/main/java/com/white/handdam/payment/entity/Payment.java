package com.white.handdam.payment.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_payment_order_id", columnNames = "order_id"),
                @UniqueConstraint(name = "uq_payment_pg_tx", columnNames = "pg_transaction_id"),
                @UniqueConstraint(name = "uq_payment_idempotency_key", columnNames = "idempotency_key")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;

    @Column(name = "pg_transaction_id", length = 255)
    private String pgTransactionId;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "customer_key", nullable = false, length = 50)
    private String customerKey;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "paid_at")
    private Instant paidAt;

    private Payment(
            Long memberId,
            Long creatorId,
            String orderId,
            String idempotencyKey,
            String customerKey,
            int amount
    ) {
        this.memberId = memberId;
        this.creatorId = creatorId;
        this.orderId = orderId;
        this.idempotencyKey = idempotencyKey;
        this.customerKey = customerKey;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public static Payment prepare(
            Long memberId,
            Long creatorId,
            String orderId,
            String idempotencyKey,
            String customerKey,
            int amount
    ) {
        validateRequired(memberId, "memberId");
        validateRequired(creatorId, "creatorId");
        validateRequired(orderId, "orderId");
        validateRequired(idempotencyKey, "idempotencyKey");
        validateRequired(customerKey, "customerKey");
        validatePositive(amount, "amount");

        return new Payment(memberId, creatorId, orderId, idempotencyKey, customerKey, amount);
    }

    public void startConfirm() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payment can start confirmation.");
        }
        status = PaymentStatus.CONFIRMING;
    }

    public void completeSuccess(
            String paymentKey,
            String paymentMethod,
            Instant paidAt,
            Long subscriptionId
    ) {
        if (status != PaymentStatus.CONFIRMING) {
            throw new IllegalStateException("Only CONFIRMING payment can be completed.");
        }
        validateRequired(paymentKey, "paymentKey");
        validateRequired(paidAt, "paidAt");
        validateRequired(subscriptionId, "subscriptionId");

        this.pgTransactionId = paymentKey;
        this.paymentMethod = paymentMethod;
        this.paidAt = paidAt;
        this.subscriptionId = subscriptionId;
        this.failureCode = null;
        this.failureMessage = null;
        this.status = PaymentStatus.SUCCESS;
    }

    public void failPending(String failureCode, String failureMessage) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payment can be failed by fail API.");
        }
        fail(failureCode, failureMessage);
    }

    public void failConfirming(String failureCode, String failureMessage) {
        if (status != PaymentStatus.CONFIRMING) {
            throw new IllegalStateException("Only CONFIRMING payment can be failed by confirm flow.");
        }
        fail(failureCode, failureMessage);
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isConfirming() {
        return status == PaymentStatus.CONFIRMING;
    }

    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isCanceled() {
        return status == PaymentStatus.CANCELED;
    }

    private void fail(String failureCode, String failureMessage) {
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.status = PaymentStatus.FAILED;
    }

    private static void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }

    private static void validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
