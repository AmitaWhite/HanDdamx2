package com.white.handdam.creator.dto.response;

/**
 * 크리에이터 공개 프로필 응답 DTO.
 * projectCount / feedCount:
 * Project 엔티티 구현 후 실제 값으로 교체
 */
public record CreatorProfileResponse(
        Long creatorId,
        Long memberId,
        String nickname,
        String profileImageUrl,
        String coverImageUrl,
        String representativeImageUrl,
        String introduction,
        String benefitsDescription,
        int subscriptionPrice,
        String subscriptionLevel,
        String subscriptionStatus,
        long subscriberCount,
        long projectCount,
        long feedCount,
        boolean isMine
) {}