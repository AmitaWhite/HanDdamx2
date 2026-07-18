package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.UpdateBoardPostRequest;
import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.service.BoardPostService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/premium-board/posts")
@RequiredArgsConstructor
public class BoardPostController {

	private final BoardPostService boardPostService;

	/**
	 * 유료 게시글 상세 조회.
	 * 권한: 게시판 크리에이터 / 작성자 / 활성 유료 구독자 (JWT 인증 필요)
	 */
	@GetMapping("/{postId}")
	public ApiResponse<BoardPostResponse> getPost(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthMember member
	) {
		return ApiResponse.success(boardPostService.getPost(postId, member.id()));
	}

	/**
	 * 유료 게시글 수정 (공식 답변 전만).
	 * 권한: 작성자
	 */
	@PatchMapping("/{postId}")
	public ApiResponse<BoardPostResponse> updatePost(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthMember member,
		@Valid @RequestBody UpdateBoardPostRequest request
	) {
		return ApiResponse.success(boardPostService.updatePost(postId, member.id(), request));
	}

	/**
	 * 유료 게시글 소프트 삭제.
	 * 권한: 작성자
	 */
	@DeleteMapping("/{postId}")
	public ApiResponse<Void> deletePost(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthMember member
	) {
		boardPostService.deletePost(postId, member.id());
		return ApiResponse.noContent();
	}
}
