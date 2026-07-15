package com.white.handdam.board.controller;

import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.service.BoardPostService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me/board-posts")
@RequiredArgsConstructor
public class MyBoardPostController {

	private final BoardPostService boardPostService;

	/**
	 * 마이페이지 — 내가 작성한 유료 게시판 글 목록.
	 * 권한: 본인(JWT 인증된 회원)만 조회
	 */
	@GetMapping
	public ApiResponse<Page<BoardPostResponse>> getMyPosts(
		@AuthenticationPrincipal AuthMember member,
		@RequestParam(required = false) BoardPostType type,
		@RequestParam(required = false) BoardPostStatus status,
		@PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.success(
			boardPostService.getMyPosts(member.id(), type, status, pageable)
		);
	}
}
