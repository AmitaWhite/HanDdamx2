package com.white.handdam.board.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBoardAnswerRequest(
	@NotBlank String content
) {
}
