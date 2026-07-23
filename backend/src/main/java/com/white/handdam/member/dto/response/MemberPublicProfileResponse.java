package com.white.handdam.member.dto.response;

import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;

import java.time.Instant;

public record MemberPublicProfileResponse(
    Long id,
    String nickname,
    String profileImageUrl,
    boolean isCreator,
    Instant createdAt
) {
    public static MemberPublicProfileResponse from(Member member) {
        return new MemberPublicProfileResponse(
            member.getId(),
            member.getNickname(),
            member.getProfileImageUrl(),
            member.getRole() == Role.CREATOR,
            member.getCreatedAt()
        );
    }
}
