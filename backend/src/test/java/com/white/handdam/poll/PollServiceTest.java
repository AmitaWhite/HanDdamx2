package com.white.handdam.poll;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.poll.dto.request.PollCreateRequest;
import com.white.handdam.poll.dto.request.PollUpdateRequest;
import com.white.handdam.poll.dto.request.PollVoteRequest;
import com.white.handdam.poll.dto.response.PollResponse;
import com.white.handdam.poll.entity.Poll;
import com.white.handdam.poll.entity.PollOption;
import com.white.handdam.poll.entity.PollVote;
import com.white.handdam.poll.exception.PollErrorCode;
import com.white.handdam.poll.repository.PollOptionRepository;
import com.white.handdam.poll.repository.PollRepository;
import com.white.handdam.poll.repository.PollVoteRepository;
import com.white.handdam.poll.service.PollService;
import com.white.handdam.project.entity.Project;
import com.white.handdam.project.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock private FeedRepository feedRepository;
    @Mock private PollRepository pollRepository;
    @Mock private PollOptionRepository pollOptionRepository;
    @Mock private PollVoteRepository pollVoteRepository;
    @Mock private ProjectRepository projectRepository;
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
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));
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
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));
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
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));
        given(pollRepository.existsByFeedId(1L)).willReturn(true); // 이미 투표 존재

        assertThatThrownBy(() -> pollService.createPoll(1L, 1L, sampleRequest(2)))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(PollErrorCode.POLL_ALREADY_EXISTS));

        // 중복이면 새 투표 저장까지 가면 안 됨
        verify(pollRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // LYJ-023 투표 조회
    // ---------------------------------------------------------------

    @Test
    @DisplayName("[LYJ-023] PUBLIC 피드 투표 조회 성공 - 미참여 시 myVotedOptionId = null")
    void getPoll_success_notVoted() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(pollOptionRepository.findByPollIdOrderByOrderIndex(1L))
                .willReturn(List.of(sampleOption(1L, "Java", 0), sampleOption(2L, "Kotlin", 1)));
        given(pollVoteRepository.findByPollIdAndMemberId(1L, 1L)).willReturn(Optional.empty());

        PollResponse result = pollService.getPoll(1L, 1L);

        assertThat(result.pollId()).isEqualTo(1L);
        assertThat(result.options()).hasSize(2);
        assertThat(result.myVotedOptionId()).isNull();    // 미참여 → null
        assertThat(result.active()).isTrue();
    }

    @Test
    @DisplayName("[LYJ-023] PUBLIC 피드 투표 조회 성공 - 참여한 경우 myVotedOptionId 반환")
    void getPoll_success_voted() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(pollOptionRepository.findByPollIdOrderByOrderIndex(1L))
                .willReturn(List.of(sampleOption(1L, "Java", 0), sampleOption(2L, "Kotlin", 1)));

        // 선택지 1번(Java)을 선택한 투표 내역
        PollVote myVote = sampleVote(1L);
        given(pollVoteRepository.findByPollIdAndMemberId(1L, 1L)).willReturn(Optional.of(myVote));

        PollResponse result = pollService.getPoll(1L, 1L);

        assertThat(result.myVotedOptionId()).isEqualTo(1L);  // 내가 선택한 optionId
    }

    @Test
    @DisplayName("[LYJ-023] 비로그인(memberId=null)은 선택지는 볼 수 있지만 myVotedOptionId = null")
    void getPoll_success_anonymous() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(pollOptionRepository.findByPollIdOrderByOrderIndex(1L))
                .willReturn(List.of(sampleOption(1L, "Java", 0), sampleOption(2L, "Kotlin", 1)));
        // memberId=null이면 pollVoteRepository 호출 자체가 안 됨 → mock 설정 불필요

        PollResponse result = pollService.getPoll(1L, null);  // 비로그인

        assertThat(result.options()).hasSize(2);
        assertThat(result.myVotedOptionId()).isNull();
    }

    @Test
    @DisplayName("[LYJ-023] 존재하지 않는 pollId 조회 시 POLL_NOT_FOUND 예외 발생")
    void getPoll_pollNotFound() {
        given(pollRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.getPoll(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(PollErrorCode.POLL_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-023] 투표가 속한 피드가 삭제됐으면 FEED_NOT_FOUND 예외 발생")
    void getPoll_feedNotFound() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.getPoll(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-023] FREE_SUBSCRIBER 피드는 비구독자에게 FEED_FORBIDDEN 예외 발생")
    void getPoll_freeSubscriberFeed_forbidden() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(sampleFeed(Visibility.FREE_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        // memberId=1L, creatorId=99L → 비소유자, level=null → FREE_SUBSCRIBER 피드는 FORBIDDEN

        assertThatThrownBy(() -> pollService.getPoll(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }

    @Test
    @DisplayName("[LYJ-023] 선택지가 orderIndex 순서대로 반환된다")
    void getPoll_optionsOrderedByIndex() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(pollOptionRepository.findByPollIdOrderByOrderIndex(1L))
                .willReturn(List.of(
                        sampleOption(1L, "Java",   0),
                        sampleOption(2L, "Kotlin", 1),
                        sampleOption(3L, "Python", 2)
                ));
        given(pollVoteRepository.findByPollIdAndMemberId(1L, 1L)).willReturn(Optional.empty());

        PollResponse result = pollService.getPoll(1L, 1L);

        assertThat(result.options()).extracting("orderIndex")
                .containsExactly(0, 1, 2);   // 순서 보장 확인
        assertThat(result.options()).extracting("optionText")
                .containsExactly("Java", "Kotlin", "Python");
    }

    // ---------------------------------------------------------------
// LYJ-024 투표 질문·종료일 수정
// ---------------------------------------------------------------

    @Test
    @DisplayName("[LYJ-024] 투표 질문·종료일 수정 성공 - pollId 반환")
    void updatePoll_success() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(1L))); // creatorId=1L = memberId=1L

        Instant newEndAt = Instant.now().plusSeconds(7200);
        Long result = pollService.updatePoll(1L, 1L,
            new PollUpdateRequest("수정된 질문", newEndAt));

        assertThat(result).isEqualTo(1L);
        assertThat(poll.getQuestion()).isEqualTo("수정된 질문");
        assertThat(poll.getEndAt()).isEqualTo(newEndAt);
    }

    @Test
    @DisplayName("[LYJ-024] question만 수정해도 endAt은 기존 값 유지")
    void updatePoll_onlyQuestion() {
        Poll poll = samplePoll(1L);
        Instant originalEndAt = poll.getEndAt();
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(1L)));

        pollService.updatePoll(1L, 1L, new PollUpdateRequest("새 질문", null));

        assertThat(poll.getQuestion()).isEqualTo("새 질문");
        assertThat(poll.getEndAt()).isEqualTo(originalEndAt); // 변경 없음
    }

    @Test
    @DisplayName("[LYJ-024] 크리에이터가 아닌 사용자가 수정 시 POLL_FORBIDDEN 예외 발생")
    void updatePoll_notOwner_forbidden() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L))); // creatorId=99L ≠ memberId=1L

        assertThatThrownBy(() -> pollService.updatePoll(1L, 1L,
            new PollUpdateRequest("수정 시도", null)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_FORBIDDEN));
    }

    @Test
    @DisplayName("[LYJ-024] 존재하지 않는 투표 수정 시 POLL_NOT_FOUND 예외 발생")
    void updatePoll_pollNotFound() {
        given(pollRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.updatePoll(999L, 1L,
            new PollUpdateRequest("질문", null)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_NOT_FOUND));
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

    // ---------------------------------------------------------------
    // LYJ-025 투표 삭제
    // ---------------------------------------------------------------

    @Test
    @DisplayName("[LYJ-025] 크리에이터가 투표 삭제 성공 - vote→option→poll 순으로 삭제")
    void deletePoll_success() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(1L))); // creatorId=1L

        pollService.deletePoll(1L, 1L);

        // 삭제 순서 검증 (InOrder)
        InOrder order = inOrder(pollVoteRepository, pollOptionRepository, pollRepository);
        order.verify(pollVoteRepository).deleteByPollId(1L);
        order.verify(pollOptionRepository).deleteByPollId(1L);
        order.verify(pollRepository).delete(poll);
    }

    @Test
    @DisplayName("[LYJ-025] 크리에이터가 아닌 사용자가 삭제 시 POLL_FORBIDDEN 예외 발생")
    void deletePoll_notOwner_forbidden() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L))); // creatorId=99L ≠ memberId=1L

        assertThatThrownBy(() -> pollService.deletePoll(1L, 1L))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_FORBIDDEN));

        // 권한 없으면 삭제 시도 없어야 함
        verify(pollRepository, never()).delete(any());
    }

    @Test
    @DisplayName("[LYJ-025] 존재하지 않는 투표 삭제 시 POLL_NOT_FOUND 예외 발생")
    void deletePoll_pollNotFound() {
        given(pollRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.deletePoll(999L, 1L))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_NOT_FOUND));
    }
    // ---------------------------------------------------------------
    // LYJ-026 투표 참여
    // ---------------------------------------------------------------

    @Test
    @DisplayName("[LYJ-026] PUBLIC 피드 비구독자 투표 성공 - weight=1, snapshot=FREE")
    void vote_public_notSubscribed_success() {
        Poll poll = samplePoll(1L); // active 상태
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L))); // 비소유자
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn(null); // 비구독
        given(pollVoteRepository.existsByPollIdAndMemberId(1L, 1L)).willReturn(false);
        given(pollOptionRepository.findById(2L))
            .willReturn(Optional.of(sampleOption(2L, "Kotlin", 1)));
        given(pollVoteRepository.save(any(PollVote.class))).willAnswer(inv -> {
            PollVote v = inv.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 100L);
            return v;
        });

        Long voteId = pollService.vote(1L, 1L, new PollVoteRequest(2L));

        assertThat(voteId).isEqualTo(100L);

        // 저장된 vote의 weight, snapshot 검증
        ArgumentCaptor<PollVote> captor = ArgumentCaptor.forClass(PollVote.class);
        verify(pollVoteRepository).save(captor.capture());
        assertThat(captor.getValue().getWeight()).isEqualTo((short) 1);
        assertThat(captor.getValue().getSubscriptionLevelSnapshot()).isEqualTo("FREE");
    }

    @Test
    @DisplayName("[LYJ-026] PAID 구독자 투표 성공 - weight=2, snapshot=PAID")
    void vote_paidSubscriber_weightTwo() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PAID_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn("PAID");
        given(pollVoteRepository.existsByPollIdAndMemberId(1L, 1L)).willReturn(false);
        given(pollOptionRepository.findById(2L))
            .willReturn(Optional.of(sampleOption(2L, "Kotlin", 1)));
        given(pollVoteRepository.save(any(PollVote.class))).willAnswer(inv -> {
            PollVote v = inv.getArgument(0);
            ReflectionTestUtils.setField(v, "id", 101L);
            return v;
        });

        pollService.vote(1L, 1L, new PollVoteRequest(2L));

        ArgumentCaptor<PollVote> captor = ArgumentCaptor.forClass(PollVote.class);
        verify(pollVoteRepository).save(captor.capture());
        assertThat(captor.getValue().getWeight()).isEqualTo((short) 2);
        assertThat(captor.getValue().getSubscriptionLevelSnapshot()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("[LYJ-026] 종료된 투표에 참여 시 POLL_CLOSED 예외 발생")
    void vote_closedPoll_throws() {
        Poll poll = sampleClosedPoll(1L); // closed=true or 만료된 투표
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.vote(1L, 1L, new PollVoteRequest(2L)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_CLOSED));

        verify(pollVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("[LYJ-026] 이미 투표한 사용자가 재참여 시 POLL_ALREADY_VOTED 예외 발생")
    void vote_alreadyVoted_throws() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn(null);
        given(pollVoteRepository.existsByPollIdAndMemberId(1L, 1L)).willReturn(true); // 이미 참여

        assertThatThrownBy(() -> pollService.vote(1L, 1L, new PollVoteRequest(2L)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_ALREADY_VOTED));
    }

    @Test
    @DisplayName("[LYJ-026] 이 투표에 속하지 않는 선택지 선택 시 POLL_OPTION_INVALID 예외 발생")
    void vote_invalidOption_throws() {
        Poll poll = samplePoll(1L); // feedId=1, pollId=1
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn(null);
        given(pollVoteRepository.existsByPollIdAndMemberId(1L, 1L)).willReturn(false);

        // optionId=99는 다른 poll(pollId=2)에 속함 → 현재 poll(1)과 불일치
        PollOption wrongOption = sampleOption(99L, "다른투표선택지", 0);
        ReflectionTestUtils.setField(wrongOption, "pollId", 2L); // pollId 강제 설정
        given(pollOptionRepository.findById(99L)).willReturn(Optional.of(wrongOption));

        assertThatThrownBy(() -> pollService.vote(1L, 1L, new PollVoteRequest(99L)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_OPTION_INVALID));
    }

    @Test
    @DisplayName("[LYJ-026] FREE_SUBSCRIBER 피드 비구독자 접근 시 FEED_FORBIDDEN 예외 발생")
    void vote_freeSubscriberFeed_notSubscribed_forbidden() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.FREE_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn(null); // 비구독

        assertThatThrownBy(() -> pollService.vote(1L, 1L, new PollVoteRequest(2L)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }

    // ---------------------------------------------------------------
    // LYJ-027 내 투표 선택지 변경
    // ---------------------------------------------------------------

    @Test
    @DisplayName("[LYJ-027] 선택지 변경 성공 - 기존 vote의 optionId가 바뀐다")
    void changeVote_success() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn(null);

        // 기존에 선택지 1번(Java)으로 투표한 상태
        PollVote existingVote = PollVote.create(1L, 1L, 1L, (short) 1, "FREE");
        ReflectionTestUtils.setField(existingVote, "id", 10L);
        given(pollVoteRepository.findByPollIdAndMemberId(1L, 1L))
            .willReturn(Optional.of(existingVote));

        // 2번(Kotlin)으로 변경 요청
        given(pollOptionRepository.findById(2L))
            .willReturn(Optional.of(sampleOption(2L, "Kotlin", 1)));

        Long result = pollService.changeVote(1L, 1L, new PollVoteRequest(2L));

        assertThat(result).isEqualTo(10L);
        assertThat(existingVote.getPollOptionId()).isEqualTo(2L); // optionId 변경 확인
    }

    @Test
    @DisplayName("[LYJ-027] 아직 투표하지 않은 사용자가 변경 시도 시 POLL_NOT_VOTED 예외 발생")
    void changeVote_notVoted_throws() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn(null);
        given(pollVoteRepository.findByPollIdAndMemberId(1L, 1L))
            .willReturn(Optional.empty()); // 투표 내역 없음

        assertThatThrownBy(() -> pollService.changeVote(1L, 1L, new PollVoteRequest(2L)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_NOT_VOTED));
    }

    @Test
    @DisplayName("[LYJ-027] 종료된 투표 선택지 변경 시도 시 POLL_CLOSED 예외 발생")
    void changeVote_closedPoll_throws() {
        Poll poll = sampleClosedPoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.changeVote(1L, 1L, new PollVoteRequest(2L)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_CLOSED));
    }

    @Test
    @DisplayName("[LYJ-027] 이 투표에 속하지 않는 선택지로 변경 시 POLL_OPTION_INVALID 예외 발생")
    void changeVote_invalidOption_throws() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn(null);

        PollVote existingVote = PollVote.create(1L, 1L, 1L, (short) 1, "FREE");
        ReflectionTestUtils.setField(existingVote, "id", 10L);
        given(pollVoteRepository.findByPollIdAndMemberId(1L, 1L))
            .willReturn(Optional.of(existingVote));

        // 다른 투표의 선택지 요청
        PollOption wrongOption = sampleOption(99L, "다른투표선택지", 0);
        ReflectionTestUtils.setField(wrongOption, "pollId", 2L); // pollId=2 → 불일치
        given(pollOptionRepository.findById(99L)).willReturn(Optional.of(wrongOption));

        assertThatThrownBy(() -> pollService.changeVote(1L, 1L, new PollVoteRequest(99L)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(PollErrorCode.POLL_OPTION_INVALID));
    }

    @Test
    @DisplayName("[LYJ-027] FREE_SUBSCRIBER 피드 비구독자가 변경 시도 시 FEED_FORBIDDEN 예외 발생")
    void changeVote_forbidden_throws() {
        Poll poll = samplePoll(1L);
        given(pollRepository.findById(1L)).willReturn(Optional.of(poll));
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.FREE_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(1L, 99L)).willReturn(null);

        assertThatThrownBy(() -> pollService.changeVote(1L, 1L, new PollVoteRequest(2L)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }

    // ---------------------------------------------------------------
    // 헬퍼 (023에 추가되는 것들)
    // ---------------------------------------------------------------

    private Poll samplePoll(Long feedId) {
        Poll poll = Poll.create(feedId, "좋아하는 언어는?", Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(poll, "id", 1L);
        return poll;
    }

    private PollOption sampleOption(Long id, String optionText, int orderIndex) {
        PollOption option = PollOption.create(1L, optionText, orderIndex);
        ReflectionTestUtils.setField(option, "id", id);
        return option;
    }

    // PollVote.create()는 LYJ-026에서 추가 예정 → ReflectionTestUtils로 직접 세팅
    private PollVote sampleVote(Long pollOptionId) {
        PollVote vote = mock(PollVote.class);
        given(vote.getPollOptionId()).willReturn(pollOptionId);
        return vote;
    }

    private Project sampleProject(Long creatorId) {
        Project project = Project.builder()
                .creatorId(creatorId)
                .categoryId(1L)
                .title("테스트 프로젝트")
                .build();
        ReflectionTestUtils.setField(project, "id", 1L);
        return project;
    }

    private Poll sampleClosedPoll(Long feedId) {
        // 이미 만료된 투표 (endAt이 과거)
        Poll poll = Poll.create(feedId, "종료된 투표?", Instant.now().minusSeconds(3600));
        ReflectionTestUtils.setField(poll, "id", 1L);
        return poll;
    }

}
