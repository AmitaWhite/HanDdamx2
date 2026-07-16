package com.white.handdam.chat.dto.response;

import com.white.handdam.chat.entity.ChatRoomStatus;
import java.time.Instant;

public record ChatRoomListItemResponse(
	Long id,
	Long creatorId,
	Long memberId,
	ChatRoomStatus status,
	Long closedBy,
	Instant closedAt,
	Instant lastMessageAt,
	Instant createdAt,
	Instant updatedAt,
	ChatMessagePreviewResponse lastMessage,
	long unreadCount
) {
}
