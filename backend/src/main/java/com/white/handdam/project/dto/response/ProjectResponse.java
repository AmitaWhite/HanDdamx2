package com.white.handdam.project.dto.response;

import java.time.Instant;

/**
 * 프로젝트 응답 DTO
 */
public record ProjectResponse(
        Long projectId,
        CreatorSummary creator,
        CategorySummary category,
        String title,
        String description,
        String coverImageUrl,
        long feedCount,
        boolean isMine,
        Instant createdAt,
        Instant updatedAt
) {
    public record CreatorSummary(Long creatorId, String nickname, String profileImageUrl) {}
    public record CategorySummary(Long categoryId, String name) {}
}