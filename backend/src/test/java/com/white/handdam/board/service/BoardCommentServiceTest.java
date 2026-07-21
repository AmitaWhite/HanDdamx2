package com.white.handdam.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.board.dto.request.CreateBoardCommentRequest;
import com.white.handdam.board.dto.request.UpdateBoardCommentRequest;
import com.white.handdam.board.dto.response.BoardCommentResponse;
import com.white.handdam.board.entity.BoardComment;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.exception.BoardErrorCode;
import com.white.handdam.board.repository.BoardCommentRepository;
import com.white.handdam.board.repository.BoardPostRepository;
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

	@Mock
	private BoardMemberNicknameResolver nicknameResolver;

	@InjectMocks
	private BoardCommentService boardCommentService;

	@org.junit.jupiter.api.BeforeEach
	void setUp() {
		lenient().when(nicknameResolver.resolve(any())).thenReturn("테스트유저");
		lenient().when(nicknameResolver.resolveAll(any())).thenReturn(java.util.Map.of());
	}

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
		willThrow(new CustomException(BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED))
			.given(boardPostService).assertCanAccessPost(post, strangerId);

		assertThatThrownBy(() -> boardCommentService.getComments(10L, strangerId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		verify(boardCommentRepository, never()).findByBoardPostIdOrderByCreatedAtAsc(any());
	}

	@Test
	@DisplayName("삭제된 게시글의 댓글 목록은 RESOURCE_NOT_FOUND를 반환한다")
	void deletedPostCannotGetComments() {
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardCommentService.getComments(10L, 1L))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_NOT_FOUND));

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
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, subscriberId);
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
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, authorId);
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
		verify(boardPostService).assertCanWriteOnPost(post, authorId);
	}

	@Test
	@DisplayName("게시판 크리에이터는 일반 댓글을 작성할 수 있다")
	void creatorCanCreateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		CreateBoardCommentRequest request = new CreateBoardCommentRequest("크리에이터 댓글");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, creatorId);
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
		verify(boardPostService).assertCanWriteOnPost(post, creatorId);
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
		willThrow(new CustomException(BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED))
			.given(boardPostService).assertCanWriteOnPost(post, strangerId);

		assertThatThrownBy(() -> boardCommentService.createComment(10L, strangerId, request))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		verify(boardCommentRepository, never()).save(any());
	}

	@Test
	@DisplayName("삭제된 게시글에는 댓글을 작성할 수 없다")
	void deletedPostCannotCreateComment() {
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() ->
			boardCommentService.createComment(10L, 1L, new CreateBoardCommentRequest("댓글"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_NOT_FOUND));

		verify(boardCommentRepository, never()).save(any());
	}

	@Test
	@DisplayName("유료 구독자는 대댓글을 작성할 수 있다")
	void paidSubscriberCanCreateReply() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long subscriberId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment parent = sampleComment(post, 100L, authorId, null, (short) 0, "부모 댓글");
		CreateBoardCommentRequest request = new CreateBoardCommentRequest("유료 구독자 대댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(parent));
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, subscriberId);
		given(boardCommentRepository.save(any(BoardComment.class))).willAnswer(invocation -> {
			BoardComment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", 300L);
			ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
			ReflectionTestUtils.setField(comment, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
			return comment;
		});

		BoardCommentResponse response = boardCommentService.createReply(100L, subscriberId, request);

		assertThat(response.id()).isEqualTo(300L);
		assertThat(response.boardPostId()).isEqualTo(10L);
		assertThat(response.memberId()).isEqualTo(subscriberId);
		assertThat(response.parentCommentId()).isEqualTo(100L);
		assertThat(response.depth()).isEqualTo((short) 1);
		assertThat(response.content()).isEqualTo("유료 구독자 대댓글");

		ArgumentCaptor<BoardComment> captor = ArgumentCaptor.forClass(BoardComment.class);
		verify(boardCommentRepository).save(captor.capture());
		assertThat(captor.getValue().getParentComment().getId()).isEqualTo(100L);
		assertThat(captor.getValue().getDepth()).isEqualTo((short) 1);
	}

	@Test
	@DisplayName("글 작성자는 대댓글을 작성할 수 있다")
	void authorCanCreateReply() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment parent = sampleComment(post, 100L, creatorId, null, (short) 0, "부모 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(parent));
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, authorId);
		given(boardCommentRepository.save(any(BoardComment.class))).willAnswer(invocation -> {
			BoardComment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", 301L);
			ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
			ReflectionTestUtils.setField(comment, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
			return comment;
		});

		BoardCommentResponse response =
			boardCommentService.createReply(100L, authorId, new CreateBoardCommentRequest("작성자 대댓글"));

		assertThat(response.memberId()).isEqualTo(authorId);
		assertThat(response.parentCommentId()).isEqualTo(100L);
		verify(boardPostService).assertCanWriteOnPost(post, authorId);
	}

	@Test
	@DisplayName("게시판 크리에이터는 대댓글을 작성할 수 있다")
	void creatorCanCreateReply() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment parent = sampleComment(post, 100L, authorId, null, (short) 0, "부모 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(parent));
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, creatorId);
		given(boardCommentRepository.save(any(BoardComment.class))).willAnswer(invocation -> {
			BoardComment comment = invocation.getArgument(0);
			ReflectionTestUtils.setField(comment, "id", 302L);
			ReflectionTestUtils.setField(comment, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
			ReflectionTestUtils.setField(comment, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
			return comment;
		});

		BoardCommentResponse response =
			boardCommentService.createReply(100L, creatorId, new CreateBoardCommentRequest("크리에이터 대댓글"));

		assertThat(response.memberId()).isEqualTo(creatorId);
		assertThat(response.depth()).isEqualTo((short) 1);
		verify(boardPostService).assertCanWriteOnPost(post, creatorId);
	}

	@Test
	@DisplayName("권한이 없으면 대댓글을 작성할 수 없다")
	void deniedCannotCreateReply() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long strangerId = 7L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment parent = sampleComment(post, 100L, authorId, null, (short) 0, "부모 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(parent));
		willThrow(new CustomException(BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED))
			.given(boardPostService).assertCanWriteOnPost(post, strangerId);

		assertThatThrownBy(() ->
			boardCommentService.createReply(100L, strangerId, new CreateBoardCommentRequest("대댓글"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		verify(boardCommentRepository, never()).save(any());
	}

	@Test
	@DisplayName("존재하지 않는 부모 댓글에는 대댓글을 작성할 수 없다")
	void missingParentCannotCreateReply() {
		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.empty());

		assertThatThrownBy(() ->
			boardCommentService.createReply(100L, 1L, new CreateBoardCommentRequest("대댓글"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_NOT_FOUND));

		verify(boardCommentRepository, never()).save(any());
	}

	@Test
	@DisplayName("대댓글에는 다시 답글을 달 수 없다")
	void cannotReplyToReply() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment root = sampleComment(post, 100L, authorId, null, (short) 0, "부모 댓글");
		BoardComment reply = sampleComment(post, 101L, creatorId, root, (short) 1, "대댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(101L)).willReturn(Optional.of(reply));

		assertThatThrownBy(() ->
			boardCommentService.createReply(101L, authorId, new CreateBoardCommentRequest("대대댓글"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_REPLY_DEPTH_EXCEEDED));

		verify(boardCommentRepository, never()).save(any());
	}

	@Test
	@DisplayName("삭제된 게시글의 댓글에는 대댓글을 작성할 수 없다")
	void deletedPostCannotCreateReply() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		post.softDelete();
		BoardComment parent = sampleComment(post, 100L, authorId, null, (short) 0, "부모 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(parent));

		assertThatThrownBy(() ->
			boardCommentService.createReply(100L, authorId, new CreateBoardCommentRequest("대댓글"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_NOT_FOUND));

		verify(boardCommentRepository, never()).save(any());
	}

	@Test
	@DisplayName("작성자는 댓글 내용을 수정할 수 있다")
	void authorCanUpdateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "원본 댓글");
		UpdateBoardCommentRequest request = new UpdateBoardCommentRequest("수정된 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, authorId);

		BoardCommentResponse response = boardCommentService.updateComment(100L, authorId, request);

		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.content()).isEqualTo("수정된 댓글");
		assertThat(response.depth()).isEqualTo((short) 0);
		assertThat(comment.getContent()).isEqualTo("수정된 댓글");
	}

	@Test
	@DisplayName("작성자는 대댓글 내용을 수정할 수 있다")
	void authorCanUpdateReply() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment root = sampleComment(post, 100L, authorId, null, (short) 0, "부모 댓글");
		BoardComment reply = sampleComment(post, 101L, creatorId, root, (short) 1, "원본 대댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(101L)).willReturn(Optional.of(reply));
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, creatorId);

		BoardCommentResponse response = boardCommentService.updateComment(
			101L,
			creatorId,
			new UpdateBoardCommentRequest("수정된 대댓글")
		);

		assertThat(response.id()).isEqualTo(101L);
		assertThat(response.parentCommentId()).isEqualTo(100L);
		assertThat(response.depth()).isEqualTo((short) 1);
		assertThat(response.content()).isEqualTo("수정된 대댓글");
	}

	@Test
	@DisplayName("작성자가 아니면 댓글을 수정할 수 없다")
	void nonAuthorCannotUpdateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long strangerId = 7L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "원본 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));

		assertThatThrownBy(() ->
			boardCommentService.updateComment(100L, strangerId, new UpdateBoardCommentRequest("수정 시도"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_EDIT_FORBIDDEN));

		assertThat(comment.getContent()).isEqualTo("원본 댓글");
	}

	@Test
	@DisplayName("유료 구독자라도 작성자가 아니면 댓글을 수정할 수 없다")
	void subscriberCannotUpdateOthersComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long subscriberId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "원본 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));

		assertThatThrownBy(() ->
			boardCommentService.updateComment(100L, subscriberId, new UpdateBoardCommentRequest("수정 시도"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_EDIT_FORBIDDEN));
	}

	@Test
	@DisplayName("게시판 참여자(크리에이터)라도 작성자가 아니면 댓글을 수정할 수 없다")
	void participantCannotUpdateOthersComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "원본 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));

		assertThatThrownBy(() ->
			boardCommentService.updateComment(100L, creatorId, new UpdateBoardCommentRequest("수정 시도"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_EDIT_FORBIDDEN));
	}

	@Test
	@DisplayName("삭제된 댓글은 수정할 수 없다")
	void deletedCommentCannotUpdate() {
		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.empty());

		assertThatThrownBy(() ->
			boardCommentService.updateComment(100L, 5L, new UpdateBoardCommentRequest("수정 시도"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_NOT_FOUND));
	}

	@Test
	@DisplayName("작성자는 댓글을 소프트 삭제할 수 있다")
	void authorCanDeleteComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "삭제할 댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, authorId);

		boardCommentService.deleteComment(100L, authorId);

		assertThat(comment.isDeleted()).isTrue();
		assertThat(comment.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("작성자는 대댓글을 소프트 삭제할 수 있다")
	void authorCanDeleteReply() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment root = sampleComment(post, 100L, authorId, null, (short) 0, "부모 댓글");
		BoardComment reply = sampleComment(post, 101L, creatorId, root, (short) 1, "삭제할 대댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(101L)).willReturn(Optional.of(reply));
		willDoNothing().given(boardPostService).assertCanWriteOnPost(post, creatorId);

		boardCommentService.deleteComment(101L, creatorId);

		assertThat(reply.isDeleted()).isTrue();
		assertThat(reply.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("작성자가 아니면 댓글을 삭제할 수 없다")
	void nonAuthorCannotDeleteComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long strangerId = 7L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));

		assertThatThrownBy(() -> boardCommentService.deleteComment(100L, strangerId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_DELETE_FORBIDDEN));

		assertThat(comment.isDeleted()).isFalse();
	}

	@Test
	@DisplayName("유료 구독자라도 작성자가 아니면 댓글을 삭제할 수 없다")
	void subscriberCannotDeleteOthersComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long subscriberId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));

		assertThatThrownBy(() -> boardCommentService.deleteComment(100L, subscriberId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_DELETE_FORBIDDEN));
	}

	@Test
	@DisplayName("게시판 참여자(크리에이터)라도 작성자가 아니면 댓글을 삭제할 수 없다")
	void participantCannotDeleteOthersComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "댓글");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));

		assertThatThrownBy(() -> boardCommentService.deleteComment(100L, creatorId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_DELETE_FORBIDDEN));
	}

	@Test
	@DisplayName("이미 삭제된 댓글은 다시 삭제할 수 없다")
	void alreadyDeletedCommentCannotDelete() {
		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardCommentService.deleteComment(100L, 5L))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_COMMENT_NOT_FOUND));
	}


	@Test
	@DisplayName("구독 해지 후에도 본인 글의 댓글 목록은 조회할 수 있다")
	void expiredAuthorCanGetCommentsOnOwnPost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment root = sampleComment(post, 100L, authorId, null, (short) 0, "내 댓글");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willDoNothing().given(boardPostService).assertCanAccessPost(post, authorId);
		given(boardCommentRepository.findByBoardPostIdOrderByCreatedAtAsc(10L)).willReturn(List.of(root));

		List<BoardCommentResponse> result = boardCommentService.getComments(10L, authorId);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().content()).isEqualTo("내 댓글");
		verify(boardPostService).assertCanAccessPost(post, authorId);
	}

	@Test
	@DisplayName("구독 해지 후에는 본인 글에도 댓글을 작성할 수 없다")
	void expiredAuthorCannotCreateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		willThrow(new CustomException(BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED))
			.given(boardPostService).assertCanWriteOnPost(post, authorId);

		assertThatThrownBy(() ->
			boardCommentService.createComment(10L, authorId, new CreateBoardCommentRequest("작성 시도"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		verify(boardCommentRepository, never()).save(any());
	}

	@Test
	@DisplayName("구독 해지 후에는 본인 댓글을 수정할 수 없다")
	void expiredAuthorCannotUpdateComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "원본");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));
		willThrow(new CustomException(BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED))
			.given(boardPostService).assertCanWriteOnPost(post, authorId);

		assertThatThrownBy(() ->
			boardCommentService.updateComment(100L, authorId, new UpdateBoardCommentRequest("수정 시도"))
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		assertThat(comment.getContent()).isEqualTo("원본");
	}

	@Test
	@DisplayName("구독 해지 후에는 본인 댓글을 삭제할 수 없다")
	void expiredAuthorCannotDeleteComment() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardComment comment = sampleComment(post, 100L, authorId, null, (short) 0, "원본");

		given(boardCommentRepository.findByIdAndDeletedFalse(100L)).willReturn(Optional.of(comment));
		willThrow(new CustomException(BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED))
			.given(boardPostService).assertCanWriteOnPost(post, authorId);

		assertThatThrownBy(() -> boardCommentService.deleteComment(100L, authorId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		assertThat(comment.isDeleted()).isFalse();
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
