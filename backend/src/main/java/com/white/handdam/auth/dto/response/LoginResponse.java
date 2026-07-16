package com.white.handdam.auth.dto.response;

import com.white.handdam.member.entity.Role;

public record LoginResponse(
        String accessToken,
        Long memberId,
        String nickname,
        Role role
) {
}
