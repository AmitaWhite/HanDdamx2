package com.white.handdam.board.dto.response;

import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import java.time.LocalDateTime;
import java.util.List;

public record BoardPostResponse(
	Long id,
	Long creatorId,
	Long memberId,
	String title,
	BoardPostType type,
	String content,
	BoardPostStatus status,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	LocalDateTime deletedAt,
	List<BoardPostImageResponse> images
) {
}
