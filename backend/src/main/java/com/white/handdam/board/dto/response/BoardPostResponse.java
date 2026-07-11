package com.white.handdam.board.dto.response;

import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import java.time.Instant;

public record BoardPostResponse(
	Long id,
	Long creatorId,
	Long memberId,
	BoardPostType type,
	String content,
	BoardPostStatus status,
	Instant createdAt,
	Instant updatedAt
) {
}
