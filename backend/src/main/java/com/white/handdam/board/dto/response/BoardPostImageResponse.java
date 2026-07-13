package com.white.handdam.board.dto.response;

import java.time.Instant;

public record BoardPostImageResponse(
	Long id,
	String url,
	String storageKey,
	String originalName,
	Long fileSize,
	String mimeType,
	int orderIndex,
	Instant createdAt
) {
}
