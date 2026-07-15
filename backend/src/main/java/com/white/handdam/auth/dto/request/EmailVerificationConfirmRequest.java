package com.white.handdam.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(
        @NotBlank
        String token
) {
}
