package com.white.handdam.board.dto.response;

import java.time.Instant;

public record BoardAnswerResponse(
	Long id,
	Long boardPostId,
	Long creatorId,
	String content,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt
) {
}
