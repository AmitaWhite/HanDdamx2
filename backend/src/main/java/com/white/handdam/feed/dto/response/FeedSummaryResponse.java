package com.white.handdam.feed.dto.response;

import com.white.handdam.category.entity.Category;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.member.entity.Member;
import com.white.handdam.project.dto.response.ProjectResponse.CategorySummary;
import com.white.handdam.project.dto.response.ProjectResponse.CreatorSummary;

import java.time.Instant;

// [LYJ-006]
public record FeedSummaryResponse(
    Long id, Long projectId, String title,
    String content, Visibility visibility, long likeCount,
    long commentCount, Instant createdAt, Instant updatedAt,
    CreatorSummary creator, CategorySummary category, boolean liked,
    String thumbnailUrl, String thumbnailType
) {
    public static FeedSummaryResponse from(
        Feed f, Member member, Category cat, boolean liked, String thumbnailUrl, String thumbnailType
    ) {
        return new FeedSummaryResponse(
            f.getId(), f.getProjectId(), f.getTitle(),
            f.getContent(), f.getVisibility(), f.getLikeCount(),
            f.getCommentCount(), f.getCreatedAt(), f.getUpdatedAt(),
            new CreatorSummary(member.getId(), member.getNickname(), member.getProfileImageUrl()),
            new CategorySummary(cat.getId(), cat.getName()),
            liked, thumbnailUrl, thumbnailType
        );
    }
}
