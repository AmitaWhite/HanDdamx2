package com.white.handdam.payment.dto.response;

import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.entity.PaymentStatus;

public record PaymentFailResponse(
        Long paymentId,
        String orderId,
        PaymentStatus status,
        String failureCode
) {

    public static PaymentFailResponse from(Payment payment) {
        return new PaymentFailResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getFailureCode()
        );
    }
}
