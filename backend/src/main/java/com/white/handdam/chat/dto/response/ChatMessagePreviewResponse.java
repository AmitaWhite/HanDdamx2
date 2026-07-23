package com.white.handdam.chat.dto.response;

import com.white.handdam.chat.entity.ChatMessageType;
import java.time.Instant;

public record ChatMessagePreviewResponse(
	Long id,
	Long senderId,
	ChatMessageType type,
	String content,
	String imageUrl,
	Instant sentAt,
	Instant readAt
) {
}
