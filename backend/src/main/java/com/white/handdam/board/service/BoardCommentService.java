package com.white.handdam.board.service;

import com.white.handdam.board.converter.BoardCommentConverter;
import com.white.handdam.board.dto.request.CreateBoardCommentRequest;
import com.white.handdam.board.dto.response.BoardCommentResponse;
import com.white.handdam.board.entity.BoardComment;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.repository.BoardCommentRepository;
import com.white.handdam.board.repository.BoardPostRepository;
import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.global.exception.CustomException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCommentService {

	private final BoardPostRepository boardPostRepository;
	private final BoardCommentRepository boardCommentRepository;
	private final BoardPostService boardPostService;

	/**
	 * 게시글 댓글·대댓글 목록 조회 (BOARD-013, BOARD-014, BOARD-018).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 게시판 접근 권한 — 크리에이터 / 작성자 / 유료 구독자
	 * 3. 댓글 flat 조회 후 트리(댓글 + replies)로 변환
	 * </pre>
	 */
	public List<BoardCommentResponse> getComments(Long postId, Long requesterId) {
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new CustomException(CommonErrorCode.RESOURCE_NOT_FOUND, "게시글을 찾을 수 없습니다."));

		boardPostService.assertCanAccessPost(post, requesterId);

		List<BoardComment> comments =
			boardCommentRepository.findByBoardPostIdOrderByCreatedAtAsc(postId);
		return BoardCommentConverter.toTree(comments);
	}

	/**
	 * 일반 댓글 작성 (BOARD-013 / LDJ-013).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 작성 권한 — 크리에이터 / 글 작성자 / 활성 유료 구독자
	 * 3. depth=0 최상위 댓글 저장 (parent_comment_id = null)
	 * </pre>
	 */
	@Transactional
	public BoardCommentResponse createComment(
		Long postId,
		Long requesterId,
		CreateBoardCommentRequest request
	) {
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new CustomException(CommonErrorCode.RESOURCE_NOT_FOUND, "게시글을 찾을 수 없습니다."));

		boardPostService.assertCanAccessPost(post, requesterId);

		BoardComment comment = BoardComment.builder()
			.boardPost(post)
			.memberId(requesterId)
			.parentComment(null)
			.depth((short) 0)
			.content(request.content())
			.build();

		BoardComment saved = boardCommentRepository.save(comment);
		return BoardCommentConverter.toResponse(saved, List.of());
	}
}
