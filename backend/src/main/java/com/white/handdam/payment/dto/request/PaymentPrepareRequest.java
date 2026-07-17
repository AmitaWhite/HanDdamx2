package com.white.handdam.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public record PaymentPrepareRequest(
        @NotNull Long creatorId
) {
}
