package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.UpdateBoardPostRequest;
import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.service.BoardPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

	/**
	 * 유료 게시글 수정 (공식 답변 전만).
	 * 권한: 작성자
	 */
	@PatchMapping("/{postId}")
	public BoardPostResponse updatePost(
		//어떤글을 수정할지
		@PathVariable Long postId,
		//누가 수정할지
		@RequestHeader("X-Member-Id") Long memberId,
		//수정할 내용
		@Valid @RequestBody UpdateBoardPostRequest request
	) {
		return boardPostService.updatePost(postId, memberId, request);
	}

	/**
	 * 유료 게시글 소프트 삭제.
	 * 권한: 작성자
	 */
	@DeleteMapping("/{postId}")
	//성고시 204 반환
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deletePost(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId
	) {
		boardPostService.deletePost(postId, memberId);
	}
}
