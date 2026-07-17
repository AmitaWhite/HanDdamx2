package com.white.handdam.payment.dto.toss;

public record TossConfirmRequest(
        String paymentKey,
        String orderId,
        int amount
) {
}
