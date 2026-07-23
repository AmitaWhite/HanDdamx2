package com.white.handdam.payment.dto.response;

import com.white.handdam.payment.entity.Payment;

public record PaymentPrepareResponse(
        Long paymentId,
        String orderId,
        String idempotencyKey,
        String customerKey,
        Long creatorId,
        String creatorNickname,
        int amount,
        String currency,
        String orderName
) {

    private static final String CURRENCY = "KRW";

    public static PaymentPrepareResponse of(
            Payment payment,
            String creatorNickname,
            String orderName
    ) {
        return new PaymentPrepareResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getIdempotencyKey(),
                payment.getCustomerKey(),
                payment.getCreatorId(),
                creatorNickname,
                payment.getAmount(),
                CURRENCY,
                orderName
        );
    }
}
