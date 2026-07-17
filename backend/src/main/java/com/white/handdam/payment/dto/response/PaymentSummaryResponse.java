package com.white.handdam.payment.dto.response;

import com.white.handdam.member.entity.Member;
import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.entity.PaymentStatus;

import java.time.Instant;

public record PaymentSummaryResponse(
        Long paymentId,
        Long creatorId,
        String creatorNickname,
        String creatorProfileImageUrl,
        int amount,
        PaymentStatus status,
        String paymentMethod,
        Instant paidAt,
        Instant createdAt
) {

    public static PaymentSummaryResponse from(Payment payment, Member creator) {
        return new PaymentSummaryResponse(
                payment.getId(),
                payment.getCreatorId(),
                creator.getNickname(),
                creator.getProfileImageUrl(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
