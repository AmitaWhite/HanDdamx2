package com.white.handdam.board.controller;

import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.service.BoardPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/premium-board/posts")
@RequiredArgsConstructor
public class BoardPostController {

	private final BoardPostService boardPostService;

	/**
	 * 유료 게시글 상세 조회.
	 * 권한: 게시판 크리에이터 / 작성자 / 활성 유료 구독자
	 */
	@GetMapping("/{postId}")
	public BoardPostResponse getPost(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId
	) {
		return boardPostService.getPost(postId, memberId);
	}
}
