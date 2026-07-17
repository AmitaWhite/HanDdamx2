package com.white.handdam.comment.service;

import com.white.handdam.comment.dto.request.FeedCommentCreateRequest;
import com.white.handdam.comment.dto.request.FeedCommentUpdateRequest;
import com.white.handdam.comment.dto.response.FeedCommentResponse;
import com.white.handdam.comment.entity.FeedComment;
import com.white.handdam.comment.exception.FeedCommentErrorCode;
import com.white.handdam.comment.repository.FeedCommentRepository;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
class FeedCommentServiceTest {

    @Mock private FeedRepository feedRepository;
    @Mock private FeedCommentRepository feedCommentRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SubscriptionLevelChecker subscriptionLevelChecker;
    @InjectMocks private FeedCommentService feedCommentService;

    // ---------------------------------------------------------------
    // LYJ-015 댓글 목록 조회
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-015] PUBLIC 피드의 댓글·대댓글을 중첩 구조로 반환한다")
    void getComments_public_returnsNestedStructure() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));

        FeedComment parent1 = sampleComment(1L, 1L, null, (short) 0, "첫 댓글");
        FeedComment parent2 = sampleComment(2L, 1L, null, (short) 0, "두번째 댓글");
        FeedComment reply   = sampleComment(3L, 1L, 1L,  (short) 1, "대댓글");
        given(feedCommentRepository.findByFeedIdAndDeletedFalseOrderByCreatedAtAsc(1L))
                .willReturn(List.of(parent1, parent2, reply));

        Member member = sampleMember(1L, "지수");
        given(memberRepository.findAllById(any())).willReturn(List.of(member));

        List<FeedCommentResponse> result = feedCommentService.getComments(1L, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).replies()).hasSize(1);
        assertThat(result.get(0).replies().get(0).content()).isEqualTo("대댓글");
        assertThat(result.get(1).replies()).isEmpty();
    }

    @Test
    @DisplayName("[LYJ-015] 존재하지 않는 피드 댓글 조회 시 FEED_NOT_FOUND 예외 발생")
    void getComments_feedNotFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.getComments(999L, null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-015] 댓글이 없으면 빈 리스트를 반환한다")
    void getComments_noComments_returnsEmpty() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(feedCommentRepository.findByFeedIdAndDeletedFalseOrderByCreatedAtAsc(1L))
                .willReturn(List.of());
        given(memberRepository.findAllById(any())).willReturn(List.of());

        List<FeedCommentResponse> result = feedCommentService.getComments(1L, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[LYJ-015] FREE_SUBSCRIBER 피드는 비회원에게 FEED_FORBIDDEN 예외 발생")
    void getComments_freeSubscriberFeed_forbiddenForGuest() {
        Feed feed = sampleFeed(Visibility.FREE_SUBSCRIBER);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));

        assertThatThrownBy(() -> feedCommentService.getComments(1L, null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }
    // ---------------------------------------------------------------
// LYJ-016 댓글 작성
// ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-016] PUBLIC 피드에 댓글 작성 성공 - 생성된 commentId 반환")
    void createComment_public_success() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));

        FeedComment saved = FeedComment.create(1L, 1L, null, (short) 0, "테스트 댓글");
        ReflectionTestUtils.setField(saved, "id", 10L);
        given(feedCommentRepository.save(any(FeedComment.class))).willReturn(saved);

        Long commentId = feedCommentService.createComment(1L, 1L, new FeedCommentCreateRequest("테스트 댓글"));

        assertThat(commentId).isEqualTo(10L);
    }

    @Test
    @DisplayName("[LYJ-016] 존재하지 않는 피드에 댓글 작성 시 FEED_NOT_FOUND 예외 발생")
    void createComment_feedNotFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.createComment(999L, 1L, new FeedCommentCreateRequest("댓글")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-016] FREE_SUBSCRIBER 피드에 비구독자 댓글 작성 시 FEED_FORBIDDEN 예외 발생")
    void createComment_freeSubscriberFeed_forbidden() {
        Feed feed = sampleFeed(Visibility.FREE_SUBSCRIBER);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));

        assertThatThrownBy(() -> feedCommentService.createComment(1L, 1L, new FeedCommentCreateRequest("댓글")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }

    // ---------------------------------------------------------------
    // LYJ-017 대댓글 작성
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-017] PUBLIC 피드에 대댓글 작성 성공 - 생성된 commentId 반환")
    void createReply_public_success() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));

        FeedComment parent = sampleComment(10L, 1L, null, (short) 0, "부모 댓글");
        given(feedCommentRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(parent));

        FeedComment savedReply = FeedComment.create(1L, 2L, 10L, (short) 1, "대댓글");
        ReflectionTestUtils.setField(savedReply, "id", 20L);
        given(feedCommentRepository.save(any(FeedComment.class))).willReturn(savedReply);

        Long commentId = feedCommentService.createReply(1L, 10L, 2L, new FeedCommentCreateRequest("대댓글"));

        assertThat(commentId).isEqualTo(20L);
    }

    @Test
    @DisplayName("[LYJ-017] 존재하지 않는 피드에 대댓글 작성 시 FEED_NOT_FOUND 예외 발생")
    void createReply_feedNotFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.createReply(999L, 10L, 1L, new FeedCommentCreateRequest("대댓글")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-017] 존재하지 않는 부모 댓글에 대댓글 작성 시 COMMENT_NOT_FOUND 예외 발생")
    void createReply_parentNotFound() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(feedCommentRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.createReply(1L, 999L, 1L, new FeedCommentCreateRequest("대댓글")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedCommentErrorCode.COMMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-017] 부모 댓글이 다른 피드 소속이면 COMMENT_NOT_FOUND 예외 발생")
    void createReply_parentBelongsToAnotherFeed() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));

        FeedComment parent = sampleComment(10L, 1L, null, (short) 0, "다른 피드 댓글");
        ReflectionTestUtils.setField(parent, "feedId", 5L); // feedId=1 요청인데 5 소속 댓글
        given(feedCommentRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(parent));

        assertThatThrownBy(() -> feedCommentService.createReply(1L, 10L, 1L, new FeedCommentCreateRequest("대댓글")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedCommentErrorCode.COMMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-017] 대댓글(depth=1)에 대댓글 작성 시 COMMENT_CANNOT_REPLY 예외 발생")
    void createReply_parentIsAlreadyReply() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));

        FeedComment parent = sampleComment(10L, 1L, 5L, (short) 1, "이미 대댓글"); // depth=1
        given(feedCommentRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(parent));

        assertThatThrownBy(() -> feedCommentService.createReply(1L, 10L, 1L, new FeedCommentCreateRequest("대댓글")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedCommentErrorCode.COMMENT_CANNOT_REPLY));
    }

    // ---------------------------------------------------------------
    // LYJ-018 댓글 수정
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-018] 댓글 수정 성공 - commentId 반환")
    void updateComment_success() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));

        FeedComment comment = sampleComment(10L, 1L, null, (short) 0, "원본 내용");
        given(feedCommentRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(comment));

        Long result = feedCommentService.updateComment(1L, 10L, 1L, new FeedCommentUpdateRequest("수정된 내용"));

        assertThat(result).isEqualTo(10L);
    }

    @Test
    @DisplayName("[LYJ-018] 존재하지 않는 피드에 댓글 수정 시 FEED_NOT_FOUND 예외 발생")
    void updateComment_feedNotFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.updateComment(999L, 10L, 1L, new FeedCommentUpdateRequest("수정")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-018] 존재하지 않는 댓글 수정 시 COMMENT_NOT_FOUND 예외 발생")
    void updateComment_commentNotFound() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(feedCommentRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedCommentService.updateComment(1L, 999L, 1L, new FeedCommentUpdateRequest("수정")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedCommentErrorCode.COMMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-018] 다른 피드 소속 댓글 수정 시 COMMENT_NOT_FOUND 예외 발생")
    void updateComment_commentBelongsToAnotherFeed() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));

        FeedComment comment = sampleComment(10L, 1L, null, (short) 0, "내용");
        ReflectionTestUtils.setField(comment, "feedId", 5L);
        given(feedCommentRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> feedCommentService.updateComment(1L, 10L, 1L, new FeedCommentUpdateRequest("수정")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedCommentErrorCode.COMMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("[LYJ-018] 작성자가 아닌 사용자가 수정 시 COMMENT_FORBIDDEN 예외 발생")
    void updateComment_notAuthor_forbidden() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));

        FeedComment comment = sampleComment(10L, 1L, null, (short) 0, "내용"); // memberId=1L 작성
        given(feedCommentRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(comment));

        // memberId=99L (다른 사람)이 수정 시도
        assertThatThrownBy(() -> feedCommentService.updateComment(1L, 10L, 99L, new FeedCommentUpdateRequest("수정")))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedCommentErrorCode.COMMENT_FORBIDDEN));
    }


    // ---------------------------------------------------------------
    // 헬퍼
    // ---------------------------------------------------------------
    private Feed sampleFeed(Visibility visibility) {
        Feed feed = Feed.create(1L, "제목", "내용", visibility);
        ReflectionTestUtils.setField(feed, "id", 1L);
        return feed;
    }

    private FeedComment sampleComment(Long id, Long memberId,
                                      Long parentId, short depth, String content) {
        FeedComment c = FeedComment.create(1L, memberId, parentId, depth, content);
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "createdAt", Instant.parse("2026-07-14T00:00:00Z"));
        ReflectionTestUtils.setField(c, "updatedAt", Instant.parse("2026-07-14T00:00:00Z"));
        return c;
    }

    private Member sampleMember(Long id, String nickname) {
        Member m = Member.createLocalMember("test@test.com", "pw", nickname);
        ReflectionTestUtils.setField(m, "id", id);
        return m;
    }
}
