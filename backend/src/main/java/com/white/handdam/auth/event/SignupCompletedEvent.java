package com.white.handdam.auth.event;

public record SignupCompletedEvent(Long memberId, String email, String nickname, String rawToken) {
}
