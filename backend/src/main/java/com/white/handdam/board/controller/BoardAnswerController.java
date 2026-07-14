package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.CreateBoardAnswerRequest;
import com.white.handdam.board.dto.request.UpdateBoardAnswerRequest;
import com.white.handdam.board.dto.response.BoardAnswerResponse;
import com.white.handdam.board.service.BoardAnswerService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BoardAnswerController {

	private final BoardAnswerService boardAnswerService;

	/**
	 * 크리에이터 공식 답변 작성.
	 * 권한: 게시판 소유 크리에이터
	 */
	@PostMapping("/api/premium-board/posts/{postId}/answer")
	public ResponseEntity<ApiResponse<BoardAnswerResponse>> createAnswer(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody CreateBoardAnswerRequest request
	) {
		BoardAnswerResponse response = boardAnswerService.createAnswer(postId, memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	/**
	 * 공식 답변 수정.
	 * 권한: 답변 작성 크리에이터
	 */
	@PatchMapping("/api/board-answers/{answerId}")
	public ApiResponse<BoardAnswerResponse> updateAnswer(
		@PathVariable Long answerId,
		@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody UpdateBoardAnswerRequest request
	) {
		return ApiResponse.success(
			boardAnswerService.updateAnswer(answerId, memberId, request)
		);
	}

	/**
	 * 공식 답변 소프트 삭제.
	 * 권한: 답변 작성 크리에이터
	 */
	@DeleteMapping("/api/board-answers/{answerId}")
	public ResponseEntity<ApiResponse<Void>> deleteAnswer(
		@PathVariable Long answerId,
		@RequestHeader("X-Member-Id") Long memberId
	) {
		boardAnswerService.deleteAnswer(answerId, memberId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.noContent());
	}
}
