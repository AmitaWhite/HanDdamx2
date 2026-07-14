package com.white.handdam.board.service;

import com.white.handdam.board.converter.BoardAnswerConverter;
import com.white.handdam.board.dto.request.CreateBoardAnswerRequest;
import com.white.handdam.board.dto.response.BoardAnswerResponse;
import com.white.handdam.board.entity.BoardAnswer;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.exception.BoardErrorCode;
import com.white.handdam.board.repository.BoardAnswerRepository;
import com.white.handdam.board.repository.BoardPostRepository;
import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardAnswerService {

	private final BoardPostRepository boardPostRepository;
	private final BoardAnswerRepository boardAnswerRepository;

	/**
	 * 크리에이터 공식 답변 작성 (BOARD-010).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 게시판 소유 크리에이터만 허용 (아니면 403)
	 * 3. 이미 답변이 있으면 409
	 * 4. board_answer 저장
	 * 5. board_post.status = ANSWERED
	 * </pre>
	 */
	@Transactional
	public BoardAnswerResponse createAnswer(
		Long postId,
		Long requesterId,
		CreateBoardAnswerRequest request
	) {
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new CustomException(CommonErrorCode.RESOURCE_NOT_FOUND, "게시글을 찾을 수 없습니다."));

		assertCanCreateAnswer(post, requesterId);

		if (boardAnswerRepository.existsByBoardPostId(postId)) {
			//"이미 공식 답변이 등록된 게시글입니다."
			throw new CustomException(BoardErrorCode.BOARD_ANSWER_ALREADY_EXISTS);
		}

		BoardAnswer answer = BoardAnswer.builder()
			.boardPost(post)
			.creatorId(requesterId)
			.content(request.content())
			.build();

		BoardAnswer saved = boardAnswerRepository.save(answer);
		post.markAnswered();

		return BoardAnswerConverter.toResponse(saved);
	}

	/**
	 * 공식 답변 작성: 게시판 소유 크리에이터만 허용.
	 */
	void assertCanCreateAnswer(BoardPost post, Long requesterId) {
		if (requesterId == null) {
			throw new CustomException(CommonErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
		}
		if (requesterId.equals(post.getCreatorId())) {
			return;
		}
		throw new CustomException(CommonErrorCode.FORBIDDEN, "공식 답변을 작성할 권한이 없습니다.");
	}
}
