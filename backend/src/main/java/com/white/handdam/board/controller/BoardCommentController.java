package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.CreateBoardCommentRequest;
import com.white.handdam.board.dto.request.UpdateBoardCommentRequest;
import com.white.handdam.board.dto.response.BoardCommentResponse;
import com.white.handdam.board.service.BoardCommentService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardCommentController {

	private final BoardCommentService boardCommentService;

	/**
	 * 게시글 댓글·대댓글 목록 조회.
	 * 권한: 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자
	 */
	@GetMapping("/api/premium-board/posts/{postId}/comments")
	public ApiResponse<List<BoardCommentResponse>> getComments(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId
	) {
		return ApiResponse.success(boardCommentService.getComments(postId, memberId));
	}

	/**
	 * 일반 댓글 작성 (BOARD-013).
	 * 권한: 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자
	 */
	@PostMapping("/api/premium-board/posts/{postId}/comments")
	public ResponseEntity<ApiResponse<BoardCommentResponse>> createComment(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody CreateBoardCommentRequest request
	) {
		BoardCommentResponse response = boardCommentService.createComment(postId, memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	/**
	 * 대댓글 작성 (BOARD-014).
	 * 권한: 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자
	 */
	@PostMapping("/api/board-comments/{commentId}/replies")
	public ResponseEntity<ApiResponse<BoardCommentResponse>> createReply(
		@PathVariable Long commentId,
		@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody CreateBoardCommentRequest request
	) {
		BoardCommentResponse response = boardCommentService.createReply(commentId, memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	/**
	 * 댓글·대댓글 수정 (LDJ-015).
	 * 권한: 해당 댓글 작성자
	 */
	@PatchMapping("/api/board-comments/{commentId}")
	public ApiResponse<BoardCommentResponse> updateComment(
		@PathVariable Long commentId,
		@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody UpdateBoardCommentRequest request
	) {
		return ApiResponse.success(
			boardCommentService.updateComment(commentId, memberId, request)
		);
	}
}
