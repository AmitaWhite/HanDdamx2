package com.white.handdam.auth.dto;

public record TokenRefreshResult(
        String accessToken,
        String refreshToken
) {
}