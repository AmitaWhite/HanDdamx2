package com.white.handdam.payment.dto.toss;

import java.time.Instant;

public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        int totalAmount,
        String method,
        Instant approvedAt
) {
}
