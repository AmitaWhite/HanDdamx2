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
import com.white.handdam.subscription.entity.SubscriptionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {
    private final FeedRepository feedRepository;
    private final SubscriptionLevelChecker subscriptionLevelChecker;

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

    // [LYJ-007] 회원 홈 피드 (구독 피드 + 카테고리 필터)
    public Slice<FeedSummaryResponse> getHomeFeed(Long memberId, Long categoryId, Pageable pageable){
        Map<Long, SubscriptionLevel> levelMap = subscriptionLevelChecker.getActiveSubscriptionLevels(memberId);
        List<Long> paidCreatorIds = levelMap.entrySet().stream()
                .filter(e -> e.getValue() == SubscriptionLevel.PAID)
                .map(e -> e.getKey())
                .toList();
        List<Long> freeCreatorIds = levelMap.entrySet().stream()
                .filter(e -> e.getValue() == SubscriptionLevel.FREE)
                .map(e -> e.getKey())
                .toList();
        // IN() 빈 리스트 Hibernate 오류 방지
        List<Long> safePaid = paidCreatorIds.isEmpty() ? List.of(-1L) : paidCreatorIds;
        List<Long> safeFree = freeCreatorIds.isEmpty() ? List.of(-1L) : freeCreatorIds;
        // TODO [LYJ-007] Project 엔티티 추가 후 수정
        // return feedRepository.findHomeFeeds(
        //          memberId, safePaid, safeFree, categoryId,
        //         List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER), pageable
        // ).map(feed -> FeedSummaryResponse.from(feed));
        return feedRepository.findByVisibilityAndDeletedFalse(Visibility.PUBLIC, pageable)
                .map(feed -> FeedSummaryResponse.from(feed));

    }

    // [LYJ-008] 전체 공개 탐색 피드 (비회원도 접근 가능)
    // categoryId : 카테고리 필터가 생겨야 적용할 수 있음
    public Slice<FeedSummaryResponse> getExploreFeeds(Long categoryId, Pageable pageable){
        // TODO [LYJ-008] Project 엔티티 추가 후 findExploreFeeds로 교체
        return feedRepository.findByVisibilityAndDeletedFalse(Visibility.PUBLIC, pageable)
                .map(feed -> FeedSummaryResponse.from(feed));
    }

    // [LYJ-009] 특정 크리에이터의 피드 목록 조회
    // 비회원: PUBLIC만, FREE 구독자 : PUBLIC+FREE_SUBSCRIBER, PAID 구독자 : 전체 조회 가능
    public Slice<FeedSummaryResponse> getCreatorFeeds(Long creatorId, Long memberId, Pageable pageable){
        String level = (memberId != null) ? subscriptionLevelChecker.getLevel(memberId, creatorId) : null;
        List<Visibility> visibilities = resolveVisibilites(level);
        // TODO [LYJ-009] Project 추가 후 아래 사용
        // return feedRepository.findByCreatorIdAndVisibilityIn(creatorId, visibilities, pageable)
        //         .map(feed -> FeedSummaryResponse.from(feed));
        return feedRepository.findByVisibilityAndDeletedFalse(Visibility.PUBLIC, pageable)
                .map(feed -> FeedSummaryResponse.from(feed));
    }

    // 구독 레벨을 접근 가능한 공개범위 목록으로 변환
    private List<Visibility> resolveVisibilites(String level){
        if("PAID".equals(level)){
            return List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER, Visibility.PAID_SUBSCRIBER);
        } else if ("FREE".equals(level)) {
            return List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER);
        }
        return List.of(Visibility.PUBLIC);
    }

    // [LYJ-010] 내 작성 피드 목록 (크리에이터 본인 전용 — 공개범위 무관 전체 조회)
    public Slice<FeedSummaryResponse> getMyFeeds(Long creatorId, Pageable pageable) {
        // TODO [LYJ-010] Project 엔티티 추가 후 아래 코드로 교체
        // return feedRepository.findByCreatorId(creatorId, pageable)
        //         .map(feed -> FeedSummaryResponse.from(feed));
        return new SliceImpl<>(List.of()); // 임시: Project 연결 전까지 빈 결과 반환
    }
}
