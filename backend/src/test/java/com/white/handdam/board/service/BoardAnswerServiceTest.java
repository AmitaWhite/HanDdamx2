package com.white.handdam.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.board.dto.request.CreateBoardAnswerRequest;
import com.white.handdam.board.dto.request.UpdateBoardAnswerRequest;
import com.white.handdam.board.dto.response.BoardAnswerResponse;
import com.white.handdam.board.entity.BoardAnswer;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.exception.BoardErrorCode;
import com.white.handdam.board.repository.BoardAnswerRepository;
import com.white.handdam.board.repository.BoardPostRepository;
import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.ErrorCode;
import java.time.Instant;
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
class BoardAnswerServiceTest {

	@Mock
	private BoardPostRepository boardPostRepository;

	@Mock
	private BoardAnswerRepository boardAnswerRepository;

	@InjectMocks
	private BoardAnswerService boardAnswerService;

	@Test
	@DisplayName("게시판 크리에이터는 공식 답변을 작성할 수 있다")
	void creatorCanCreateAnswer() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		CreateBoardAnswerRequest request = new CreateBoardAnswerRequest("공식 답변입니다.");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardAnswerRepository.existsByBoardPostId(10L)).willReturn(false);
		given(boardAnswerRepository.save(any(BoardAnswer.class))).willAnswer(invocation -> {
			BoardAnswer answer = invocation.getArgument(0);
			ReflectionTestUtils.setField(answer, "id", 50L);
			ReflectionTestUtils.setField(answer, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
			ReflectionTestUtils.setField(answer, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
			return answer;
		});

		BoardAnswerResponse result = boardAnswerService.createAnswer(10L, creatorId, request);

		assertThat(result.id()).isEqualTo(50L);
		assertThat(result.boardPostId()).isEqualTo(10L);
		assertThat(result.creatorId()).isEqualTo(creatorId);
		assertThat(result.content()).isEqualTo("공식 답변입니다.");
		assertThat(post.getStatus()).isEqualTo(BoardPostStatus.ANSWERED);

		ArgumentCaptor<BoardAnswer> captor = ArgumentCaptor.forClass(BoardAnswer.class);
		verify(boardAnswerRepository).save(captor.capture());
		assertThat(captor.getValue().getContent()).isEqualTo("공식 답변입니다.");
	}

	@Test
	@DisplayName("게시판 크리에이터가 아니면 공식 답변을 작성할 수 없다")
	void nonCreatorCannotCreateAnswer() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long subscriberId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		CreateBoardAnswerRequest request = new CreateBoardAnswerRequest("공식 답변입니다.");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardAnswerService.createAnswer(10L, subscriberId, request))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.FORBIDDEN));

		verify(boardAnswerRepository, never()).save(any());
		assertThat(post.getStatus()).isEqualTo(BoardPostStatus.WAITING);
	}

	@Test
	@DisplayName("이미 공식 답변이 있으면 BOARD_ANSWER_ALREADY_EXISTS를 반환한다")
	void cannotCreateDuplicateAnswer() {
		Long creatorId = 1L;
		BoardPost post = samplePost(creatorId, 5L);
		CreateBoardAnswerRequest request = new CreateBoardAnswerRequest("두 번째 답변");

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardAnswerRepository.existsByBoardPostId(10L)).willReturn(true);

		assertThatThrownBy(() -> boardAnswerService.createAnswer(10L, creatorId, request))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_ANSWER_ALREADY_EXISTS));

		verify(boardAnswerRepository, never()).save(any());
	}

	@Test
	@DisplayName("삭제된 게시글에는 공식 답변을 작성할 수 없다")
	void cannotCreateAnswerOnDeletedPost() {
		CreateBoardAnswerRequest request = new CreateBoardAnswerRequest("답변");
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardAnswerService.createAnswer(10L, 1L, request))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.RESOURCE_NOT_FOUND));

		verify(boardAnswerRepository, never()).save(any());
	}

	@Test
	@DisplayName("비로그인은 공식 답변을 작성할 수 없다")
	void guestCannotCreateAnswer() {
		BoardPost post = samplePost(1L, 5L);
		CreateBoardAnswerRequest request = new CreateBoardAnswerRequest("답변");
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardAnswerService.createAnswer(10L, null, request))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.UNAUTHORIZED));

		verify(boardAnswerRepository, never()).save(any());
	}

	@Test
	@DisplayName("답변 작성 크리에이터는 공식 답변을 수정할 수 있다")
	void creatorCanUpdateAnswer() {
		Long creatorId = 1L;
		BoardAnswer answer = sampleAnswer(creatorId);
		UpdateBoardAnswerRequest request = new UpdateBoardAnswerRequest("수정된 공식 답변");

		given(boardAnswerRepository.findByIdAndDeletedFalse(50L)).willReturn(Optional.of(answer));

		BoardAnswerResponse result = boardAnswerService.updateAnswer(50L, creatorId, request);

		assertThat(result.id()).isEqualTo(50L);
		assertThat(result.content()).isEqualTo("수정된 공식 답변");
		assertThat(answer.getContent()).isEqualTo("수정된 공식 답변");
	}

	@Test
	@DisplayName("답변 작성자가 아니면 공식 답변을 수정할 수 없다")
	void nonCreatorCannotUpdateAnswer() {
		Long creatorId = 1L;
		Long otherId = 99L;
		BoardAnswer answer = sampleAnswer(creatorId);
		UpdateBoardAnswerRequest request = new UpdateBoardAnswerRequest("수정된 공식 답변");

		given(boardAnswerRepository.findByIdAndDeletedFalse(50L)).willReturn(Optional.of(answer));

		assertThatThrownBy(() -> boardAnswerService.updateAnswer(50L, otherId, request))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.FORBIDDEN));

		assertThat(answer.getContent()).isEqualTo("공식 답변입니다.");
	}

	@Test
	@DisplayName("삭제된 공식 답변은 수정 시 RESOURCE_NOT_FOUND를 반환한다")
	void deletedAnswerCannotBeUpdated() {
		UpdateBoardAnswerRequest request = new UpdateBoardAnswerRequest("수정된 공식 답변");
		given(boardAnswerRepository.findByIdAndDeletedFalse(50L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardAnswerService.updateAnswer(50L, 1L, request))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	@Test
	@DisplayName("비로그인은 공식 답변을 수정할 수 없다")
	void guestCannotUpdateAnswer() {
		BoardAnswer answer = sampleAnswer(1L);
		UpdateBoardAnswerRequest request = new UpdateBoardAnswerRequest("수정된 공식 답변");
		given(boardAnswerRepository.findByIdAndDeletedFalse(50L)).willReturn(Optional.of(answer));

		assertThatThrownBy(() -> boardAnswerService.updateAnswer(50L, null, request))
			.satisfies(ex -> assertErrorCode(ex, CommonErrorCode.UNAUTHORIZED));
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

	private BoardAnswer sampleAnswer(Long creatorId) {
		BoardPost post = samplePost(creatorId, 5L);
		BoardAnswer answer = BoardAnswer.builder()
			.boardPost(post)
			.creatorId(creatorId)
			.content("공식 답변입니다.")
			.build();
		ReflectionTestUtils.setField(answer, "id", 50L);
		ReflectionTestUtils.setField(answer, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
		ReflectionTestUtils.setField(answer, "updatedAt", Instant.parse("2026-07-15T00:00:00Z"));
		return answer;
	}
}
