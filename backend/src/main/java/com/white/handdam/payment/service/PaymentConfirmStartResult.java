package com.white.handdam.payment.service;

import com.white.handdam.payment.dto.response.PaymentConfirmResponse;

public record PaymentConfirmStartResult(
        PaymentConfirmCommand command,
        PaymentConfirmResponse existingResponse
) {

    public static PaymentConfirmStartResult requiresToss(PaymentConfirmCommand command) {
        return new PaymentConfirmStartResult(command, null);
    }

    public static PaymentConfirmStartResult alreadySucceeded(PaymentConfirmResponse existingResponse) {
        return new PaymentConfirmStartResult(null, existingResponse);
    }

    public boolean alreadySucceeded() {
        return existingResponse != null;
    }
}
