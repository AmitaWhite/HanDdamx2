package com.white.handdam.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentFailRequest(
        @NotBlank String orderId,
        @NotBlank @Size(max = 100) String code,
        @Size(max = 1000) String message
) {
}
