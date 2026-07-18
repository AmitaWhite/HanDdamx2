package com.white.handdam.like;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.like.dto.response.FeedLikeResponse;
import com.white.handdam.like.entity.FeedLike;
import com.white.handdam.like.exception.FeedLikeErrorCode;
import com.white.handdam.like.repository.FeedLikeRepository;
import com.white.handdam.like.service.FeedLikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FeedLikeServiceTest {

    @Mock private FeedRepository feedRepository;
    @Mock private FeedLikeRepository feedLikeRepository;
    @InjectMocks private FeedLikeService feedLikeService;

    // ---------------------------------------------------------------
    // LYJ-020 좋아요 등록
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-020] 좋아요 등록 성공 - likeCount 증가 및 liked=true 반환")
    void addLike_success() {
        Feed feed = sampleFeed(Visibility.PUBLIC, 4L); // likeCount=4
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(feedLikeRepository.existsByFeedIdAndMemberId(1L, 1L)).willReturn(false);
        given(feedLikeRepository.save(any(FeedLike.class))).willReturn(FeedLike.create(1L, 1L));

        FeedLikeResponse result = feedLikeService.addLike(1L, 1L);

        assertThat(result.likeCount()).isEqualTo(5L);
        assertThat(result.liked()).isTrue();
    }

    @Test
    @DisplayName("[LYJ-020] 존재하지 않는 피드 좋아요 시 FEED_NOT_FOUND 예외 발생")
    void addLike_feedNotFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedLikeService.addLike(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-020] 이미 좋아요한 피드에 다시 좋아요 시 ALREADY_LIKED 예외 발생")
    void addLike_alreadyLiked() {
        Feed feed = sampleFeed(Visibility.PUBLIC, 5L);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(feedLikeRepository.existsByFeedIdAndMemberId(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> feedLikeService.addLike(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedLikeErrorCode.ALREADY_LIKED));
    }

    // ---------------------------------------------------------------
    // LYJ-021 좋아요 취소
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-021] 좋아요 취소 성공 - likeCount 감소 및 liked=false 반환")
    void cancelLike_success() {
        Feed feed = sampleFeed(Visibility.PUBLIC, 5L); // likeCount=5
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(feedLikeRepository.findByFeedIdAndMemberId(1L, 1L)).willReturn(Optional.of(FeedLike.create(1L, 1L)));

        FeedLikeResponse result = feedLikeService.cancelLike(1L, 1L);

        assertThat(result.likeCount()).isEqualTo(4L);
        assertThat(result.liked()).isFalse();
    }

    @Test
    @DisplayName("[LYJ-021] 존재하지 않는 피드 좋아요 취소 시 FEED_NOT_FOUND 예외 발생")
    void cancelLike_feedNotFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedLikeService.cancelLike(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-021] 좋아요하지 않은 피드 취소 시 NOT_LIKED 예외 발생")
    void cancelLike_notLiked() {
        Feed feed = sampleFeed(Visibility.PUBLIC, 0L);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(feedLikeRepository.findByFeedIdAndMemberId(1L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedLikeService.cancelLike(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedLikeErrorCode.NOT_LIKED));
    }

    // ---------------------------------------------------------------
    // 헬퍼
    // ---------------------------------------------------------------
    private Feed sampleFeed(Visibility visibility, long likeCount) {
        Feed feed = Feed.create(1L, "제목", "내용", visibility);
        ReflectionTestUtils.setField(feed, "id", 1L);
        ReflectionTestUtils.setField(feed, "likeCount", likeCount);
        return feed;
    }
}
