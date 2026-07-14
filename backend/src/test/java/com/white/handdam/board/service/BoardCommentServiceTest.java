package com.white.handdam.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.board.dto.request.CreateBoardCommentRequest;
import com.white.handdam.board.dto.response.BoardCommentResponse;
import com.white.handdam.board.entity.BoardComment;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.repository.BoardCommentRepository;
import com.white.handdam.board.repository.BoardPostRepository;
import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoardCommentServiceTest {

	@Mock
	private BoardPostRepository boardPostRepository;

	@Mock
	private BoardCommentRepository boardCommentRepository;

	@Mock
	private BoardPostService boardPostService;

	@InjectMocks
	private BoardCommentService boardCommentService;

	@Test
	@DisplayName("접근 가능한 사용자는 댓글·대댓글 트리를 조회할 수 있다")
	void canGetCommentTree() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long requesterId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment root = sampleComment(post, 100L, authorId, null, (short) 0, "댓글");
		BoardComment reply = sampleComment(post, 101L, creatorId, root, (short) 1, "대댓글");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willDoNothing().given(boardPostService).assertCanAccessPost(post, requesterId);
		given(boardCommentRepository.findByBoardPostIdOrderByCreatedAtAsc(10L))
			.willReturn(List.of(root, reply));

		List<BoardCommentResponse> result = boardCommentService.getComments(10L, requesterId);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().id()).isEqualTo(100L);
		assertThat(result.getFirst().content()).isEqualTo("댓글");
		assertThat(result.getFirst().replies()).hasSize(1);
		assertThat(result.getFirst().replies().getFirst().id()).isEqualTo(101L);
		assertThat(result.getFirst().replies().getFirst().parentCommentId()).isEqualTo(100L);
		assertThat(result.getFirst().replies().getFirst().depth()).isEqualTo((short) 1);
	}

	@Test
	@DisplayName("권한이 없으면 댓글 목록을 조회할 수 없다")
	void deniedCannotGetComments() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long strangerId = 7L;
		BoardPost post = samplePost(creatorId, authorId);

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willThrow(new CustomException(CommonErrorCode.SUBSCRIPTION_REQUIRED))
			.given(boardPostService).assertCanAccessPost(post, strangerId);

		assertThatThrownBy(() -> boardCommentService.getComments(10L, strangerId))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.SUBSCRIPTION_REQUIRED));

		verify(boardCommentRepository, never()).findByBoardPostIdOrderByCreatedAtAsc(any());
	}

	@Test
	@DisplayName("삭제된 게시글의 댓글 목록은 RESOURCE_NOT_FOUND를 반환한다")
	void deletedPostCannotGetComments() {
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardCommentService.getComments(10L, 1L))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.RESOURCE_NOT_FOUND));

		verify(boardCommentRepository, never()).findByBoardPostIdOrderByCreatedAtAsc(any());
	}

	@Test
	@DisplayName("유료 구독자는 일반 댓글을 작성할 수 있다")
	void paidSubscriberCanCreateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long subscriberId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		CreateBoardCommentRequest request = new CreateBoardCommentRequest("유료 구독자 댓글");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willDoNothing().given(boardPostService).assertCanAccessPost(post, subscriberId);
		given(boardCommentRepository.save(any(BoardComment.class))).willAnswer(invocation -> {
			BoardComment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", 200L);
			ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
			ReflectionTestUtils.setField(comment, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
			return comment;
		});

		BoardCommentResponse response = boardCommentService.createComment(10L, subscriberId, request);

		assertThat(response.id()).isEqualTo(200L);
		assertThat(response.boardPostId()).isEqualTo(10L);
		assertThat(response.memberId()).isEqualTo(subscriberId);
		assertThat(response.parentCommentId()).isNull();
		assertThat(response.depth()).isEqualTo((short) 0);
		assertThat(response.content()).isEqualTo("유료 구독자 댓글");
		assertThat(response.replies()).isEmpty();

		ArgumentCaptor<BoardComment> captor = ArgumentCaptor.forClass(BoardComment.class);
		verify(boardCommentRepository).save(captor.capture());
		assertThat(captor.getValue().getParentComment()).isNull();
		assertThat(captor.getValue().getDepth()).isEqualTo((short) 0);
	}

	@Test
	@DisplayName("글 작성자는 일반 댓글을 작성할 수 있다")
	void authorCanCreateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		CreateBoardCommentRequest request = new CreateBoardCommentRequest("작성자 댓글");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willDoNothing().given(boardPostService).assertCanAccessPost(post, authorId);
		given(boardCommentRepository.save(any(BoardComment.class))).willAnswer(invocation -> {
			BoardComment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", 201L);
			ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
			ReflectionTestUtils.setField(comment, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
			return comment;
		});

		BoardCommentResponse response = boardCommentService.createComment(10L, authorId, request);

		assertThat(response.memberId()).isEqualTo(authorId);
		assertThat(response.content()).isEqualTo("작성자 댓글");
		verify(boardPostService).assertCanAccessPost(post, authorId);
	}

	@Test
	@DisplayName("게시판 크리에이터는 일반 댓글을 작성할 수 있다")
	void creatorCanCreateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		CreateBoardCommentRequest request = new CreateBoardCommentRequest("크리에이터 댓글");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willDoNothing().given(boardPostService).assertCanAccessPost(post, creatorId);
		given(boardCommentRepository.save(any(BoardComment.class))).willAnswer(invocation -> {
			BoardComment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", 202L);
			ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
			ReflectionTestUtils.setField(comment, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
			return comment;
		});

		BoardCommentResponse response = boardCommentService.createComment(10L, creatorId, request);

		assertThat(response.memberId()).isEqualTo(creatorId);
		assertThat(response.content()).isEqualTo("크리에이터 댓글");
		verify(boardPostService).assertCanAccessPost(post, creatorId);
	}

	@Test
	@DisplayName("권한이 없으면 댓글을 작성할 수 없다")
	void deniedCannotCreateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long strangerId = 7L;
		BoardPost post = samplePost(creatorId, authorId);
		CreateBoardCommentRequest request = new CreateBoardCommentRequest("권한 없는 댓글");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willThrow(new CustomException(CommonErrorCode.SUBSCRIPTION_REQUIRED))
			.given(boardPostService).assertCanAccessPost(post, strangerId);

		assertThatThrownBy(() -> boardCommentService.createComment(10L, strangerId, request))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.SUBSCRIPTION_REQUIRED));

		verify(boardCommentRepository, never()).save(any());
	}

	@Test
	@DisplayName("삭제된 게시글에는 댓글을 작성할 수 없다")
	void deletedPostCannotCreateComment() {
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() ->
			boardCommentService.createComment(10L, 1L, new CreateBoardCommentRequest("댓글"))
		).satisfies(ex -> assertErrorCode(ex, CommonErrorCode.RESOURCE_NOT_FOUND));

		verify(boardCommentRepository, never()).save(any());
	}

	private static void assertErrorCode(Throwable thrown, ErrorCode expected) {
		assertThat(thrown).isInstanceOf(CustomException.class);
		assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(expected);
	}

	private BoardPost samplePost(Long creatorId, Long memberId) {
		BoardPost post = BoardPost.builder()
			.creatorId(creatorId)
			.memberId(memberId)
			.title("테스트 제목")
			.type(BoardPostType.QUESTION)
			.content("테스트 질문")
			.status(BoardPostStatus.WAITING)
			.build();
		ReflectionTestUtils.setField(post, "id", 10L);
		ReflectionTestUtils.setField(post, "createdAt", Instant.parse("2026-07-11T00:00:00Z"));
		ReflectionTestUtils.setField(post, "updatedAt", Instant.parse("2026-07-11T00:00:00Z"));
		return post;
	}

	private BoardComment sampleComment(
		BoardPost post,
		Long id,
		Long memberId,
		BoardComment parent,
		short depth,
		String content
	) {
		BoardComment comment = BoardComment.builder()
			.boardPost(post)
			.memberId(memberId)
			.parentComment(parent)
			.depth(depth)
			.content(content)
			.build();
		ReflectionTestUtils.setField(comment, "id", id);
		ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
		ReflectionTestUtils.setField(comment, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
		return comment;
	}
}
