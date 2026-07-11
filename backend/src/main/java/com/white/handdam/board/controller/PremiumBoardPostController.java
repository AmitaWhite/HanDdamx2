package com.white.handdam.board.controller;

import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.service.BoardPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/creators/{creatorId}/premium-board/posts")
@RequiredArgsConstructor
public class PremiumBoardPostController {

	private final BoardPostService boardPostService;

	/**
	 * 크리에이터별 유료 게시판 게시글 목록 조회.
	 * 인증 인프라 연동 전: X-Member-Id 헤더로 호출자 식별 (이후 JWT로 교체).
	 */
	@GetMapping
	public Page<BoardPostResponse> getPosts(
		//어느 크리에이터의 유료 게시판인지
		@PathVariable Long creatorId,
		@RequestHeader("X-Member-Id") Long memberId,
		@RequestParam(required = false) BoardPostType type,
		@RequestParam(required = false) BoardPostStatus status,
		@PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return boardPostService.getPostsByCreator(creatorId, memberId, type, status, pageable);
	}
}
