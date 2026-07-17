package com.white.handdam.payment.service;

public record PaymentConfirmCommand(
        Long paymentId,
        Long memberId,
        Long creatorId,
        String paymentKey,
        String orderId,
        int amount,
        String idempotencyKey
) {
}
