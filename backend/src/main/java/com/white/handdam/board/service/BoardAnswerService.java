package com.white.handdam.board.service;

import com.white.handdam.board.converter.BoardAnswerConverter;
import com.white.handdam.board.dto.request.CreateBoardAnswerRequest;
import com.white.handdam.board.dto.request.UpdateBoardAnswerRequest;
import com.white.handdam.board.dto.response.BoardAnswerResponse;
import com.white.handdam.board.entity.BoardAnswer;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.exception.BoardErrorCode;
import com.white.handdam.board.repository.BoardAnswerRepository;
import com.white.handdam.board.repository.BoardPostRepository;
import com.white.handdam.global.exception.CustomException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardAnswerService {

	private final BoardPostRepository boardPostRepository;
	private final BoardAnswerRepository boardAnswerRepository;
	private final BoardPostService boardPostService;

	/**
	 * 게시글의 활성 공식 답변 조회.
	 * 권한: 게시글 접근 가능자(크리에이터/작성자/유료 구독자)와 동일.
	 */
	public BoardAnswerResponse getAnswer(Long postId, Long requesterId) {
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_POST_NOT_FOUND));
		boardPostService.assertCanAccessPost(post, requesterId);

		BoardAnswer answer = boardAnswerRepository.findByBoardPostIdAndDeletedFalse(postId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_ANSWER_NOT_FOUND));
		return BoardAnswerConverter.toResponse(answer);
	}

	/**
	 * 크리에이터 공식 답변 작성 (BOARD-010).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 게시판 소유 크리에이터만 허용 (아니면 403)
	 * 3. 활성 답변이 있으면 409
	 * 4. 소프트 삭제된 답변이 있으면 복구(restore) 후 재등록
	 *    — UNIQUE(board_post_id) 때문에 INSERT 대신 기존 행 재사용
	 * 5. 없으면 board_answer 신규 저장
	 * 6. board_post.status = ANSWERED
	 * </pre>
	 */
	@Transactional
	public BoardAnswerResponse createAnswer(
		Long postId,
		Long requesterId,
		CreateBoardAnswerRequest request
	) {
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		assertCanCreateAnswer(post, requesterId);

		if (boardAnswerRepository.existsByBoardPostIdAndDeletedFalse(postId)) {
			throw new CustomException(BoardErrorCode.BOARD_ANSWER_ALREADY_EXISTS);
		}

		// 활성 답변은 위에서 이미 막았으므로, 여기 있으면 소프트 삭제된 행이다.
		// UNIQUE(board_post_id) 때문에 INSERT 대신 기존 행을 재사용한다.
		Optional<BoardAnswer> existing = boardAnswerRepository.findByBoardPostId(postId);
		BoardAnswer saved;
		if (existing.isPresent()) {// 조회 결과가 있으면 true
			// 소프트 삭제 답변 복구: content/creator 갱신, deleted=false
			BoardAnswer answer = existing.get();
			answer.restore(request.content(), requesterId);
			saved = answer; // 영속 엔티티라 save 호출 없이 dirty checking 으로 UPDATE
		} else {
			// 해당 게시글에 답변 행이 한 번도 없으면 신규 INSERT
			saved = boardAnswerRepository.save(
				BoardAnswer.builder()
					.boardPost(post)
					.creatorId(requesterId)
					.content(request.content())
					.build()
			);
		}

		// 게시글 상태를 WAITING → ANSWERED 로 전환
		post.markAnswered();
		return BoardAnswerConverter.toResponse(saved);
	}


	/**
	 * 공식 답변 작성: 게시판 소유 크리에이터만 허용.
	 */
	void assertCanCreateAnswer(BoardPost post, Long requesterId) {
		BoardOwnershipAsserter.assertOwner(
			post.getCreatorId(),
			requesterId,
			BoardErrorCode.BOARD_ANSWER_CREATE_FORBIDDEN
		);
	}

	/**
	 * 크리에이터 공식 답변 수정 (BOARD-011).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 답변 조회 (없으면 404)
	 * 2. 답변 작성 크리에이터만 허용 (아니면 403)
	 * 3. content 갱신
	 * </pre>
	 */
	@Transactional
	public BoardAnswerResponse updateAnswer(
		Long answerId,
		Long requesterId,
		UpdateBoardAnswerRequest request
	) {
		BoardAnswer answer = boardAnswerRepository.findByIdAndDeletedFalse(answerId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_ANSWER_NOT_FOUND));

		// 2) 작성 크리에이터만 수정 허용 (아니면 403)
		assertCanEditAnswer(answer, requesterId);
		// 3) 요청 body의 content 로 본문 갱신
		answer.updateContent(request.content());

		return BoardAnswerConverter.toResponse(answer);
	}

	/**
	 * 크리에이터 공식 답변 소프트 삭제 (BOARD-012).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 답변 조회 (없으면 404)
	 * 2. 답변 작성 크리에이터만 허용 (아니면 403)
	 * 3. is_deleted=true, deleted_at 설정
	 * 4. board_post.status = WAITING 으로 되돌림
	 * </pre>
	 */
	@Transactional
	public void deleteAnswer(Long answerId, Long requesterId) {
		BoardAnswer answer = boardAnswerRepository.findByIdAndDeletedFalse(answerId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_ANSWER_NOT_FOUND));

		assertCanEditAnswer(answer, requesterId);
		answer.softDelete();
		answer.getBoardPost().markWaiting();
	}

	/**
	 * 공식 답변 수정·삭제: 해당 답변을 작성한 크리에이터만 허용.
	 */
	void assertCanEditAnswer(BoardAnswer answer, Long requesterId) {
		BoardOwnershipAsserter.assertOwner(
			answer.getCreatorId(),
			requesterId,
			BoardErrorCode.BOARD_ANSWER_EDIT_FORBIDDEN
		);
	}
}
