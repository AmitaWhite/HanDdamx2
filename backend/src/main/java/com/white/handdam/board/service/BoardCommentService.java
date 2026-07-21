package com.white.handdam.board.service;

import com.white.handdam.board.converter.BoardCommentConverter;
import com.white.handdam.board.dto.request.CreateBoardCommentRequest;
import com.white.handdam.board.dto.request.UpdateBoardCommentRequest;
import com.white.handdam.board.dto.response.BoardCommentResponse;
import com.white.handdam.board.entity.BoardComment;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.exception.BoardErrorCode;
import com.white.handdam.board.repository.BoardCommentRepository;
import com.white.handdam.board.repository.BoardPostRepository;
import com.white.handdam.global.exception.CustomException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
	private final BoardMemberNicknameResolver nicknameResolver;

	/**
	 * 게시글 댓글·대댓글 목록 조회 (BOARD-013, BOARD-014, BOARD-018).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 조회 권한 — 크리에이터 / 글 작성자 / 활성 유료 구독자 (구독 해지 후에도 본인 글은 조회 가능)
	 * 3. 댓글 flat 조회 후 트리(댓글 + replies)로 변환
	 * </pre>
	 */
	public List<BoardCommentResponse> getComments(Long postId, Long requesterId) {
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		boardPostService.assertCanAccessPost(post, requesterId);

		List<BoardComment> comments =
			boardCommentRepository.findByBoardPostIdOrderByCreatedAtAsc(postId);
		Set<Long> memberIds = comments.stream()
			.map(BoardComment::getMemberId)
			.collect(Collectors.toSet());
		Map<Long, String> nicknameMap = nicknameResolver.resolveAll(memberIds);
		return BoardCommentConverter.toTree(comments, nicknameMap);
	}

	/**
	 * 일반 댓글 작성 (BOARD-013 / LDJ-013).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 쓰기 권한 — 크리에이터 / 활성 유료 구독자 (구독 해지 시 작성 불가)
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
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_POST_NOT_FOUND));

		boardPostService.assertCanWriteOnPost(post, requesterId);

		BoardComment comment = BoardComment.builder()
			.boardPost(post)
			.memberId(requesterId)
			.parentComment(null)
			.depth((short) 0)
			.content(request.content())
			.build();

		BoardComment saved = boardCommentRepository.save(comment);
		return BoardCommentConverter.toResponse(
			saved,
			List.of(),
			Map.of(saved.getMemberId(), nicknameResolver.resolve(requesterId))
		);
	}

	/**
	 * 대댓글 작성 (BOARD-014 / LDJ-014).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 부모 댓글 조회 (없으면 404)
	 * 2. 부모는 최상위 댓글(depth=0)만 허용 (아니면 400)
	 * 3. 부모 댓글이 속한 게시글이 삭제되지 않았는지 확인 (아니면 404)
	 * 4. 쓰기 권한 — 크리에이터 / 활성 유료 구독자 (구독 해지 시 작성 불가)
	 * 5. depth=1 대댓글 저장 (parent_comment_id = 부모, board_post_id = 부모와 동일)
	 * </pre>
	 */
	@Transactional
	public BoardCommentResponse createReply(
		Long commentId,
		Long requesterId,
		CreateBoardCommentRequest request
	) {
		BoardComment parent = boardCommentRepository.findByIdAndDeletedFalse(commentId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_COMMENT_NOT_FOUND));

		if (parent.getDepth() != 0) {
			throw new CustomException(BoardErrorCode.BOARD_COMMENT_REPLY_DEPTH_EXCEEDED);
		}

		BoardPost post = parent.getBoardPost();
		if (post.isDeleted()) {
			throw new CustomException(BoardErrorCode.BOARD_POST_NOT_FOUND);
		}

		boardPostService.assertCanWriteOnPost(post, requesterId);

		BoardComment reply = BoardComment.builder()
			.boardPost(post)
			.memberId(requesterId)
			.parentComment(parent)
			.depth((short) 1)
			.content(request.content())
			.build();

		BoardComment saved = boardCommentRepository.save(reply);
		return BoardCommentConverter.toResponse(
			saved,
			List.of(),
			Map.of(saved.getMemberId(), nicknameResolver.resolve(requesterId))
		);
	}

	/**
	 * 댓글·대댓글 수정 (LDJ-015).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 댓글 조회 (없으면 404)
	 * 2. 작성자만 허용 (아니면 403)
	 * 3. 활성 유료 구독(또는 크리에이터) 확인 — 구독 해지 시 수정 불가
	 * 4. content 갱신
	 * </pre>
	 */
	@Transactional
	public BoardCommentResponse updateComment(
		Long commentId,
		Long requesterId,
		UpdateBoardCommentRequest request
	) {
		BoardComment comment = boardCommentRepository.findByIdAndDeletedFalse(commentId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_COMMENT_NOT_FOUND));

		assertCanEditComment(comment, requesterId);
		boardPostService.assertCanWriteOnPost(comment.getBoardPost(), requesterId);
		comment.updateContent(request.content());

		return BoardCommentConverter.toResponse(
			comment,
			List.of(),
			Map.of(comment.getMemberId(), nicknameResolver.resolve(comment.getMemberId()))
		);
	}

	/**
	 * 댓글·대댓글 소프트 삭제 (LDJ-016).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 댓글 조회 (없으면 404)
	 * 2. 작성자만 허용 (아니면 403)
	 * 3. 활성 유료 구독(또는 크리에이터) 확인 — 구독 해지 시 삭제 불가
	 * 4. is_deleted=true, deleted_at 설정
	 * </pre>
	 */
	@Transactional
	public void deleteComment(Long commentId, Long requesterId) {
		BoardComment comment = boardCommentRepository.findByIdAndDeletedFalse(commentId)
			.orElseThrow(() -> new CustomException(BoardErrorCode.BOARD_COMMENT_NOT_FOUND));

		assertCanDeleteComment(comment, requesterId);
		boardPostService.assertCanWriteOnPost(comment.getBoardPost(), requesterId);
		comment.softDelete();
	}

	/**
	 * 댓글·대댓글 수정: 해당 댓글 작성자만 허용.
	 */
	void assertCanEditComment(BoardComment comment, Long requesterId) {
		BoardOwnershipAsserter.assertOwner(
			comment.getMemberId(),
			requesterId,
			BoardErrorCode.BOARD_COMMENT_EDIT_FORBIDDEN
		);
	}

	/**
	 * 댓글·대댓글 삭제: 해당 댓글 작성자만 허용.
	 */
	void assertCanDeleteComment(BoardComment comment, Long requesterId) {
		BoardOwnershipAsserter.assertOwner(
			comment.getMemberId(),
			requesterId,
			BoardErrorCode.BOARD_COMMENT_DELETE_FORBIDDEN
		);
	}
}
