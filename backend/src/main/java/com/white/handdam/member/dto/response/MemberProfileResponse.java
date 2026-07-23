package com.white.handdam.member.dto.response;

import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.OAuthProvider;
import com.white.handdam.member.entity.Role;

import java.time.Instant;

public record MemberProfileResponse(
    Long id, String email, String nickname,
    String profileImageUrl, Role role, OAuthProvider oauthProvider,
    Instant createdAt
) {

    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
            member.getId(),
            member.getEmail(),
            member.getNickname(),
            member.getProfileImageUrl(),
            member.getRole(),
            member.getOauthProvider(),
            member.getCreatedAt()
        );
    }

}
