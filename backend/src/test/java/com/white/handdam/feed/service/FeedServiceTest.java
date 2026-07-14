package com.white.handdam.feed.service;

import com.white.handdam.feed.dto.request.FeedCreateRequest;
import com.white.handdam.feed.dto.request.FeedUpdateRequest;
import com.white.handdam.feed.dto.response.FeedDetailResponse;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    @Test
    @DisplayName("소유자가 피드를 수정하면 수정된 feedId를 반환한다")
    void updateFeed_success() {
        // TODO: Project 엔티티 추가 후 isOwner=true 경로 테스트
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
    @Test
    @DisplayName("소유자가 아닌 경우 FEED_FORBIDDEN 예외가 발생한다")
    void updateFeed_forbidden() {
        FeedUpdateRequest request = new FeedUpdateRequest("새제목", "새내용", Visibility.PUBLIC);
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        // 현재 isOwner=false 스텁이므로 항상 FORBIDDEN
        assertThatThrownBy(() -> feedService.updateFeed(1L, 99L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }

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