package com.white.handdam.member.dto.response;

import com.white.handdam.member.entity.Role;

public record MemberSummaryResponse(
    Long id,
    String nickname,
    String profileImageUrl,
    Role role,
    String introduction, // Creator가 아니면 null
    long myBoardCommentCount,
    long subscribingCount // 내가 구독 중인 크리에이터 수
) {
}
