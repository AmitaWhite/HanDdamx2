package com.white.handdam.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
