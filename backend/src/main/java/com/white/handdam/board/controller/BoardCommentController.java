package com.white.handdam.board.controller;

import com.white.handdam.board.dto.response.BoardCommentResponse;
import com.white.handdam.board.service.BoardCommentService;
import com.white.handdam.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/premium-board/posts/{postId}/comments")
@RequiredArgsConstructor
public class BoardCommentController {

	private final BoardCommentService boardCommentService;

	/**
	 * 게시글 댓글·대댓글 목록 조회.
	 * 권한: 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자
	 */
	@GetMapping
	public ApiResponse<List<BoardCommentResponse>> getComments(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId
	) {
		return ApiResponse.success(boardCommentService.getComments(postId, memberId));
	}
}
