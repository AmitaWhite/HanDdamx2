package com.white.handdam.auth.dto.request;

import com.white.handdam.auth.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetConfirmRequest(
        @NotBlank
        String token,
        @ValidPassword
        String newPassword
) {
}
