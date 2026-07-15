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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import org.springframework.data.domain.SliceImpl;
import java.util.List;
import java.util.Map;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {
    @Mock
    private FeedRepository feedRepository;
    @Mock
    private SubscriptionLevelChecker subscriptionLevelChecker;
    @InjectMocks
    private FeedService feedService;
    // ---------------------------------------------------------------
    // LYJ-001 피드 작성
    // ---------------------------------------------------------------
    @Test
    @DisplayName("피드를 작성하면 저장된 피드의 ID를 반환한다")
    void createFeed_success() {
        FeedCreateRequest request = new FeedCreateRequest(1L, "제목", "내용", Visibility.PUBLIC);
        Feed saved = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.save(any(Feed.class))).willReturn(saved);
        Long result = feedService.createFeed(1L, request);
        assertThat(result).isEqualTo(1L);
        verify(feedRepository).save(any(Feed.class));
    }
    // ---------------------------------------------------------------
    // LYJ-002 + LYJ-030 피드 상세 조회 + 공개범위 잠금
    // ---------------------------------------------------------------
    @Test
    @DisplayName("PUBLIC 피드는 누구나 내용을 볼 수 있다")
    void getFeed_public_visibleToAll() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        FeedDetailResponse result = feedService.getFeed(1L, null);
        assertThat(result.locked()).isFalse();
        assertThat(result.content()).isEqualTo("내용");
    }
    @Test
    @DisplayName("PUBLIC 피드는 로그인 없이도 볼 수 있다")
    void getFeed_public_noLogin() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        FeedDetailResponse result = feedService.getFeed(1L, null); // memberId=null
        assertThat(result.locked()).isFalse();
    }
    @Test
    @DisplayName("FREE_SUBSCRIBER 피드는 비구독자에게 잠긴다")
    void getFeed_freeSubscriber_lockedForNonSubscriber() {
        Feed feed = sampleFeed(Visibility.FREE_SUBSCRIBER);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        FeedDetailResponse result = feedService.getFeed(1L, 6L); // 비구독자
        assertThat(result.locked()).isTrue();
        assertThat(result.content()).isNull();
        assertThat(result.requiredLevel()).isEqualTo("FREE_SUBSCRIBER");
    }
    @Test
    @DisplayName("PAID_SUBSCRIBER 피드는 비구독자에게 잠긴다")
    void getFeed_paidSubscriber_lockedForNonSubscriber() {
        Feed feed = sampleFeed(Visibility.PAID_SUBSCRIBER);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        FeedDetailResponse result = feedService.getFeed(1L, 6L);
        assertThat(result.locked()).isTrue();
        assertThat(result.content()).isNull();
        assertThat(result.requiredLevel()).isEqualTo("PAID_SUBSCRIBER");
    }
    @Test
    @DisplayName("존재하지 않는 피드 조회 시 FEED_NOT_FOUND 예외가 발생한다")
    void getFeed_notFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> feedService.getFeed(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }
// ---------------------------------------------------------------
// LYJ-003 피드 수정
// ---------------------------------------------------------------
    @Disabled("TODO [LYJ-003] Project 엔티티 추가 후 소유권 검증 활성화되면 해제")
    @Test
    @DisplayName("피드를 수정하면 수정된 feedId를 반환하고 필드가 변경된다")
    void updateFeed_success() {
        FeedUpdateRequest request = new FeedUpdateRequest("새제목", "새내용", Visibility.FREE_SUBSCRIBER);
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        Long result = feedService.updateFeed(1L, 1L, request);
        assertThat(result).isEqualTo(1L);
        assertThat(feed.getTitle()).isEqualTo("새제목");
        assertThat(feed.getContent()).isEqualTo("새내용");
        assertThat(feed.getVisibility()).isEqualTo(Visibility.FREE_SUBSCRIBER);
    }
    @Test
    @DisplayName("존재하지 않는 피드 수정 시 FEED_NOT_FOUND 예외가 발생한다")
    void updateFeed_notFound() {
        FeedUpdateRequest request = new FeedUpdateRequest("새제목", "새내용", Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> feedService.updateFeed(999L, 1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }
    // ---------------------------------------------------------------
    // LYJ-004 피드 프로젝트 이동
    // ---------------------------------------------------------------
    @Disabled("TODO [LYJ-004] Project 엔티티 추가 후 소유권 검증 활성화되면 해제")
    @Test
    @DisplayName("피드의 프로젝트를 이동하면 feedId를 반환하고 projectId가 변경된다")
    void moveFeedProject_success() {
        FeedMoveProjectRequest request = new FeedMoveProjectRequest(2L);
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        Long result = feedService.moveFeedProject(1L, 1L, request);
        assertThat(result).isEqualTo(1L);
        assertThat(feed.getProjectId()).isEqualTo(2L);
    }
    @Test
    @DisplayName("존재하지 않는 피드 프로젝트 이동 시 FEED_NOT_FOUND 예외가 발생한다")
    void moveFeedProject_notFound() {
        FeedMoveProjectRequest request = new FeedMoveProjectRequest(2L);
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> feedService.moveFeedProject(999L, 1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }
    // ---------------------------------------------------------------
    // LYJ-005 피드 소프트 삭제
    // ---------------------------------------------------------------
    @Test
    @DisplayName("존재하지 않는 피드 삭제 시 FEED_NOT_FOUND 예외가 발생한다")
    void deleteFeed_notFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> feedService.deleteFeed(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }
    // ---------------------------------------------------------------
    // LYJ-007 회원 홈 피드
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-007] 홈 피드 조회 시 구독 레벨 체커를 호출하고 피드 목록을 반환한다")
    void getHomeFeed_callsSubscriptionCheckerAndReturnsFeeds() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(subscriptionLevelChecker.getActiveSubscriptionLevels(1L)).willReturn(Map.of());
        given(feedRepository.findByVisibilityAndDeletedFalse(eq(Visibility.PUBLIC), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(feed)));

        Slice<FeedSummaryResponse> result = feedService.getHomeFeed(1L, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
        verify(subscriptionLevelChecker).getActiveSubscriptionLevels(1L);
    }

    @Test
    @DisplayName("[LYJ-007] 구독이 없으면 빈 구독 맵으로 조회한다")
    void getHomeFeed_noSubscription_usesEmptyMap() {
        given(subscriptionLevelChecker.getActiveSubscriptionLevels(1L)).willReturn(Map.of());
        given(feedRepository.findByVisibilityAndDeletedFalse(any(), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));

        Slice<FeedSummaryResponse> result = feedService.getHomeFeed(1L, null, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
        verify(subscriptionLevelChecker).getActiveSubscriptionLevels(1L);
    }

    // TODO [LYJ-007] Project 엔티티 + findHomeFeeds 구현 후 아래 테스트 활성화
    // @Test
    // @DisplayName("[LYJ-007] PAID 구독자는 PAID_SUBSCRIBER 피드를 볼 수 있다")
    // void getHomeFeed_paid_seesAllVisibilities() { ... }
    //
    // @Test
    // @DisplayName("[LYJ-007] FREE 구독자는 PUBLIC, FREE_SUBSCRIBER 피드를 볼 수 있다")
    // void getHomeFeed_free_seesFreeAndPublic() { ... }
    //
    // @Test
    // @DisplayName("[LYJ-007] categoryId 필터가 쿼리에 전달된다")
    // void getHomeFeed_withCategoryFilter() { ... }

    // ---------------------------------------------------------------
    // 헬퍼
    // ---------------------------------------------------------------
    private Feed sampleFeed(Visibility visibility) {
        Feed feed = Feed.create(1L, "제목", "내용", visibility);
        ReflectionTestUtils.setField(feed, "id", 1L);
        ReflectionTestUtils.setField(feed, "createdAt", Instant.parse("2026-07-14T00:00:00Z"));
        ReflectionTestUtils.setField(feed, "updatedAt", Instant.parse("2026-07-14T00:00:00Z"));
        return feed;
    }
}