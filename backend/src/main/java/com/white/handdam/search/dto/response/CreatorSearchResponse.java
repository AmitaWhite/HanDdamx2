package com.white.handdam.search.dto.response;

/**
 * 크리에이터 검색 결과 경량 DTO.
 * 둘러보기 상단 "크리에이터" 매칭 섹션에서 아바타+닉네임 표시에 필요한 최소 필드만 담는다.
 * (구독/카운트 조회를 수반하는 무거운 CreatorProfileResponse 대신 사용.)
 *
 * creatorId = Member.id (공개 프로필 라우트 GET /api/creators/{creatorId} 가 member id 기준).
 */
public record CreatorSearchResponse(
        Long creatorId,
        String nickname,
        String profileImageUrl,
        String introduction
) {}
