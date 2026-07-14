package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.CreateBoardAnswerRequest;
import com.white.handdam.board.dto.response.BoardAnswerResponse;
import com.white.handdam.board.service.BoardAnswerService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/premium-board/posts/{postId}/answer")
@RequiredArgsConstructor
public class BoardAnswerController {

	private final BoardAnswerService boardAnswerService;

	/**
	 * 크리에이터 공식 답변 작성.
	 * 권한: 게시판 소유 크리에이터
	 */
	@PostMapping
	public ResponseEntity<ApiResponse<BoardAnswerResponse>> createAnswer(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody CreateBoardAnswerRequest request
	) {
		BoardAnswerResponse response = boardAnswerService.createAnswer(postId, memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}
}
