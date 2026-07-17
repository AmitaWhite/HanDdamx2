package com.white.handdam.payment.dto.response;

import com.white.handdam.member.entity.Member;
import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.entity.PaymentStatus;

import java.time.Instant;

public record PaymentDetailResponse(
        Long paymentId,
        Long subscriptionId,
        Long creatorId,
        String creatorNickname,
        String creatorProfileImageUrl,
        int amount,
        PaymentStatus status,
        String paymentMethod,
        String failureCode,
        Instant paidAt,
        Instant createdAt
) {

    public static PaymentDetailResponse from(Payment payment, Member creator) {
        return new PaymentDetailResponse(
                payment.getId(),
                payment.getSubscriptionId(),
                payment.getCreatorId(),
                creator.getNickname(),
                creator.getProfileImageUrl(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getFailureCode(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
