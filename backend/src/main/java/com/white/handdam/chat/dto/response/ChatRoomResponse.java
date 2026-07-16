package com.white.handdam.chat.dto.response;

import com.white.handdam.chat.entity.ChatRoomStatus;
import java.time.Instant;

public record ChatRoomResponse(
	Long id,
	Long creatorId,
	Long memberId,
	ChatRoomStatus status,
	Long closedBy,
	Instant closedAt,
	Instant lastMessageAt,
	Instant createdAt,
	Instant updatedAt
) {
}
