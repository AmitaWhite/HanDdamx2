package com.white.handdam.auth.event;

import com.white.handdam.auth.entity.VerificationPurpose;

public record VerificationEmailRequestedEvent(
        String email,
        String nickname,
        String rawToken,
        VerificationPurpose purpose) {
}