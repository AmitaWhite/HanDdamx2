package com.white.handdam.auth.dto.response;

import com.white.handdam.member.entity.Member;

public record SignupResponse(
        Long memberId,
        String email,
        String nickname
) {
    public static SignupResponse from(Member member) {
        return new SignupResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname());
    }
}
