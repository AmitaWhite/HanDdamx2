package com.white.handdam.board.dto.response;

import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import java.time.Instant;
import java.util.List;

public record BoardPostResponse(
	Long id,
	Long creatorId,
	Long memberId,
	String memberNickname,
	String title,
	BoardPostType type,
	String content,
	BoardPostStatus status,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt,
	List<BoardPostImageResponse> images
) {
}
