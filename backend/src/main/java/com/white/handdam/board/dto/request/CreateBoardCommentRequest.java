package com.white.handdam.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardCommentRequest(
	@NotBlank @Size(max = 1000) String content
) {
}
