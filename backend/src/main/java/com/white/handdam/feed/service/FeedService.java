package com.white.handdam.feed.service;

import com.white.handdam.feed.dto.request.FeedCreateRequest;
import com.white.handdam.feed.dto.request.FeedMoveProjectRequest;
import com.white.handdam.feed.dto.request.FeedUpdateRequest;
import com.white.handdam.feed.dto.response.FeedDetailResponse;
import com.white.handdam.feed.dto.response.FeedSummaryResponse;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {
    private final FeedRepository feedRepository;
    // [LYJ-001] 피드 작성
    @Transactional
    public Long createFeed(Long memberId, FeedCreateRequest request) {
        // TODO [LYJ-001] Project 엔티티 생성 후 소유권 검증 활성화
        Feed feed = Feed.create(request.projectId(), request.title(), request.content(), request.visibility());
        return feedRepository.save(feed).getId();
    }

    // [LYJ-002] 피드 상세 조회 + 공개범위 잠금 처리 [LYJ-030]
    public FeedDetailResponse getFeed(Long feedId, Long memberId){
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        // TODO [LYJ-030] Project 엔티티 추가

        boolean isOwner = false; // TODO: Project 추가 후 교체
        String level = null;
        return canAccess(feed.getVisibility(), level, isOwner)
                ? FeedDetailResponse.visible(feed)
                : FeedDetailResponse.locked(feed);
    }

    // [LYJ-030] 피드 공개범위
    private boolean canAccess(Visibility visibility, String level, boolean isOwner) {
        if (isOwner) return true;
        return switch (visibility) {
            case PUBLIC          -> true;
            case FREE_SUBSCRIBER -> "FREE".equals(level) || "PAID".equals(level);
            case PAID_SUBSCRIBER -> "PAID".equals(level);
        };
    }

    // [LYJ-003] 피드 수정
    @Transactional
    public Long updateFeed(Long feedId, Long memberId, FeedUpdateRequest request) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        // TODO [LYJ-003] Project 엔티티 생성 후 소유권 검증 활성화
        boolean isOwner = false; // TODO: Project 추가 후 교체
        if (!isOwner) throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        feed.update(request.title(), request.content(), request.visibility());
        return feed.getId();
    }

    // [LYJ-004] 피드 프로젝트 이동
    @Transactional
    public Long moveFeedProject(Long feedId, Long memberId, FeedMoveProjectRequest request){
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        // TODO [LYJ-004] Project 엔티티 생성 후 소유권 검증 활성화
        boolean isOwner = false; // TODO: Project 추가 후 교체
        if(!isOwner) throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        feed.moveProject(request.projectId());
        return feed.getId();
    }

    // [LYJ-005] 피드 소프트 삭제
    @Transactional
    public void deleteFeed(Long feedId, Long memberId){
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        // TODO [LYJ-005] Project 엔티티 생성 후 소유권 검증 활성화
        boolean isOwner = false; // TODO: Project 추가 후 교체
        if(!isOwner) throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        feed.delete();
    }

    // [LYJ-006] 최근 PUBLIC 피드 목록
    public Slice<FeedSummaryResponse> getPublicFeeds(Pageable pageable){
        return feedRepository.findByVisibilityAndDeletedFalse(Visibility.PUBLIC, pageable)
                .map(feed -> FeedSummaryResponse.from(feed));
    }

}
