package com.white.handdam.auth.dto;

import com.white.handdam.member.entity.Role;

public record LoginResult(
        String accessToken,
        String refreshToken,
        Long memberId,
        String nickname,
        Role role
) {
}
