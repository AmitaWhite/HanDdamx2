package com.white.handdam.chat.dto.response;

import com.white.handdam.chat.entity.ChatMessageType;
import java.time.Instant;

public record ChatMessageResponse(
	Long id,
	Long chatRoomId,
	Long senderId,
	ChatMessageType type,
	String content,
	String imageUrl,
	String imageOriginalName,
	Instant readAt,
	Instant sentAt
) {
}
