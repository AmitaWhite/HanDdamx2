package com.white.handdam.feed.dto.response;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;

import java.time.Instant;

// [LYJ-006]
// 프로젝트랑 연결 후 creatorid, creatorname 추가 필요
public record FeedSummaryResponse (
    Long id, Long projectId, String title,
    String content, Visibility visibility, long likeCount,
    long commentCount, Instant createdAt, Instant updatedAt
) {
    public static FeedSummaryResponse from(Feed f){
        return new FeedSummaryResponse(
          f.getId(), f.getProjectId(), f.getTitle(),
          f.getContent(), f.getVisibility(), f.getLikeCount(),
          f.getCommentCount(), f.getCreatedAt(), f.getUpdatedAt()
        );
    }
}
