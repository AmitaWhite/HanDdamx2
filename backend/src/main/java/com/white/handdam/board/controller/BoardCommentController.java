package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.CreateBoardCommentRequest;
import com.white.handdam.board.dto.request.UpdateBoardCommentRequest;
import com.white.handdam.board.dto.response.BoardCommentResponse;
import com.white.handdam.board.service.BoardCommentService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardCommentController {

	private final BoardCommentService boardCommentService;

	/**
	 * 게시글 댓글·대댓글 목록 조회.
	 * 권한: 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자 (JWT 인증 필요)
	 */
	@GetMapping("/api/premium-board/posts/{postId}/comments")
	public ApiResponse<List<BoardCommentResponse>> getComments(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthMember member
	) {
		return ApiResponse.success(boardCommentService.getComments(postId, member.id()));
	}

	/**
	 * 일반 댓글 작성 (BOARD-013).
	 * 권한: 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/api/premium-board/posts/{postId}/comments")
	public ApiResponse<BoardCommentResponse> createComment(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthMember member,
		@Valid @RequestBody CreateBoardCommentRequest request
	) {
		return ApiResponse.success(boardCommentService.createComment(postId, member.id(), request));
	}

	/**
	 * 대댓글 작성 (BOARD-014).
	 * 권한: 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/api/board-comments/{commentId}/replies")
	public ApiResponse<BoardCommentResponse> createReply(
		@PathVariable Long commentId,
		@AuthenticationPrincipal AuthMember member,
		@Valid @RequestBody CreateBoardCommentRequest request
	) {
		return ApiResponse.success(boardCommentService.createReply(commentId, member.id(), request));
	}

	/**
	 * 댓글·대댓글 수정 (LDJ-015).
	 * 권한: 해당 댓글 작성자
	 */
	@PatchMapping("/api/board-comments/{commentId}")
	public ApiResponse<BoardCommentResponse> updateComment(
		@PathVariable Long commentId,
		@AuthenticationPrincipal AuthMember member,
		@Valid @RequestBody UpdateBoardCommentRequest request
	) {
		return ApiResponse.success(
			boardCommentService.updateComment(commentId, member.id(), request)
		);
	}

	/**
	 * 댓글·대댓글 소프트 삭제 (LDJ-016).
	 * 권한: 해당 댓글 작성자
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/api/board-comments/{commentId}")
	public ApiResponse<Void> deleteComment(
		@PathVariable Long commentId,
		@AuthenticationPrincipal AuthMember member
	) {
		boardCommentService.deleteComment(commentId, member.id());
		return ApiResponse.noContent();
	}
}
