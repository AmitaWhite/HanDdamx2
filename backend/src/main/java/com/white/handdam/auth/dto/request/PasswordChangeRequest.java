package com.white.handdam.auth.dto.request;

import com.white.handdam.auth.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank
        String currentPassword,
        @ValidPassword
        String newPassword
) {
}
