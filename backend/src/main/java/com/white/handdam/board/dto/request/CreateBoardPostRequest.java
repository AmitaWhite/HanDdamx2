package com.white.handdam.board.dto.request;

import com.white.handdam.board.entity.BoardPostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBoardPostRequest(
	@NotBlank @Size(max = 255) String title,
	@NotNull BoardPostType type,
	@NotBlank String content
) {
}
