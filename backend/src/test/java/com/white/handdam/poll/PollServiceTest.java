package com.white.handdam.poll;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.poll.dto.request.PollCreateRequest;
import com.white.handdam.poll.entity.Poll;
import com.white.handdam.poll.exception.PollErrorCode;
import com.white.handdam.poll.repository.PollOptionRepository;
import com.white.handdam.poll.repository.PollRepository;
import com.white.handdam.poll.service.PollService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock private FeedRepository feedRepository;
    @Mock private PollRepository pollRepository;
    @Mock private PollOptionRepository pollOptionRepository;
    @Mock private SubscriptionLevelChecker subscriptionLevelChecker;
    @InjectMocks private PollService pollService;

    // ---------------------------------------------------------------
    // LYJ-022 투표 생성
    // ---------------------------------------------------------------

    @Test
    @DisplayName("[LYJ-022] 투표 생성 성공 - pollId 반환")
    void createPoll_success() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(pollRepository.existsByFeedId(1L)).willReturn(false);

        // save() 호출 시 JPA가 id를 채워주는 것을 Mockito로 흉내냄
        // poll.getId()를 이후 PollOption.create() 에서 사용하기 때문에 반드시 필요
        given(pollRepository.save(any(Poll.class))).willAnswer(invocation -> {
            Poll p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 10L);
            return p;
        });

        Long pollId = pollService.createPoll(1L, 1L, sampleRequest(3));

        assertThat(pollId).isEqualTo(10L);
    }

    @Test
    @DisplayName("[LYJ-022] 투표 생성 시 선택지가 요청 개수만큼 저장된다")
    void createPoll_success_optionsSavedCorrectly() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(pollRepository.existsByFeedId(1L)).willReturn(false);
        given(pollRepository.save(any(Poll.class))).willAnswer(invocation -> {
            Poll p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 10L);
            return p;
        });

        pollService.createPoll(1L, 1L, sampleRequest(3)); // 선택지 3개

        // saveAll 에 전달된 리스트 캡처
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(pollOptionRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(3); // 선택지 3개 저장 확인
    }

    @Test
    @DisplayName("[LYJ-022] 피드가 없으면 FEED_NOT_FOUND 예외 발생")
    void createPoll_feedNotFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.createPoll(999L, 1L, sampleRequest(2)))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));

        // 피드가 없으면 투표 저장까지 가면 안 됨
        verify(pollRepository, never()).save(any());
    }

    @Test
    @DisplayName("[LYJ-022] 피드에 투표가 이미 있으면 POLL_ALREADY_EXISTS 예외 발생")
    void createPoll_pollAlreadyExists() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(pollRepository.existsByFeedId(1L)).willReturn(true); // 이미 투표 존재

        assertThatThrownBy(() -> pollService.createPoll(1L, 1L, sampleRequest(2)))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(PollErrorCode.POLL_ALREADY_EXISTS));

        // 중복이면 새 투표 저장까지 가면 안 됨
        verify(pollRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // 헬퍼
    // ---------------------------------------------------------------

    private Feed sampleFeed(Visibility visibility) {
        Feed feed = Feed.create(1L, "테스트 피드", "내용", visibility);
        ReflectionTestUtils.setField(feed, "id", 1L);
        return feed;
    }

    // optionCount 개수만큼 선택지를 가진 요청 생성
    private PollCreateRequest sampleRequest(int optionCount) {
        List<String> options = switch (optionCount) {
            case 2 -> List.of("Java", "Kotlin");
            case 3 -> List.of("Java", "Kotlin", "Python");
            default -> List.of("선택지1", "선택지2");
        };
        return new PollCreateRequest(
                "좋아하는 언어는?",
                Instant.now().plusSeconds(3600), // 1시간 후 종료
                options
        );
    }
}
