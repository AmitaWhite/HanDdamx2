package com.white.handdam.payment.dto.response;

import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.entity.PaymentStatus;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;

import java.time.Instant;

public record PaymentConfirmResponse(
        Long paymentId,
        String orderId,
        String paymentKey,
        int amount,
        PaymentStatus status,
        String paymentMethod,
        Instant paidAt,
        Long subscriptionId,
        SubscriptionLevel subscriptionLevel,
        Instant currentPeriodStartAt,
        Instant currentPeriodEndAt
) {

    public static PaymentConfirmResponse of(Payment payment, Subscription subscription) {
        return new PaymentConfirmResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPgTransactionId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getPaidAt(),
                subscription.getId(),
                subscription.getSubscriptionLevel(),
                subscription.getCurrentPeriodStartAt(),
                subscription.getCurrentPeriodEndAt()
        );
    }
}
