package com.white.handdam.feed.dto.response;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;

import java.time.Instant;

// [LYJ-002] 피드 상세 응답
// locked가 true면 content=null, requiredLevel에 필요한 구독 등급 표시
public record FeedDetailResponse(
        Long id, Long projectId, String title,
        String content, Visibility visibility, boolean locked,
        String requiredLevel, long likeCount, long commentCount,
        Instant createdAt, Instant updatedAt
) {
    // 접근 가능 - 내용 전체 반환
    public static FeedDetailResponse visible(Feed f){
        return new FeedDetailResponse(
                f.getId(), f.getProjectId(), f.getTitle(),
                f.getContent(), f.getVisibility(), false,
                null, f.getLikeCount(), f.getCommentCount(),
                f.getCreatedAt(), f.getUpdatedAt()
        );
    }

    // 접근 불가 - content null, locked= true
    public static FeedDetailResponse locked(Feed f){
        return new FeedDetailResponse(
                f.getId(), f.getProjectId(), f.getTitle(),
                null, f.getVisibility(), true,
                f.getVisibility().name(), f.getLikeCount(), f.getCommentCount(),
                f.getCreatedAt(), f.getUpdatedAt()
        );
    }
}
