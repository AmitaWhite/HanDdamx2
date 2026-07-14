package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.UpdateBoardAnswerRequest;
import com.white.handdam.board.dto.response.BoardAnswerResponse;
import com.white.handdam.board.service.BoardAnswerService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/board-answers")
@RequiredArgsConstructor
public class BoardAnswersController {

	private final BoardAnswerService boardAnswerService;

	/**
	 * 공식 답변 수정.
	 * 권한: 답변 작성 크리에이터
	 */
	@PatchMapping("/{answerId}")
	public ApiResponse<BoardAnswerResponse> updateAnswer(
		@PathVariable Long answerId,
		@RequestHeader("X-Member-Id") Long memberId,
		@Valid @RequestBody UpdateBoardAnswerRequest request
	) {
		return ApiResponse.success(
			boardAnswerService.updateAnswer(answerId, memberId, request)
		);
	}
}
