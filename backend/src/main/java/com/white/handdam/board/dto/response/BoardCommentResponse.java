package com.white.handdam.board.dto.response;

import java.time.Instant;
import java.util.List;

public record BoardCommentResponse(
	Long id,
	Long boardPostId,
	Long memberId,
	Long parentCommentId,
	short depth,
	String content,
	boolean deleted,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt,
	List<BoardCommentResponse> replies
) {
}
