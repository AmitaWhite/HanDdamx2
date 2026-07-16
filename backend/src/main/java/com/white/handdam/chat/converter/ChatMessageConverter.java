package com.white.handdam.chat.converter;

import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.entity.ChatMessage;

public final class ChatMessageConverter {

	private ChatMessageConverter() {
	}

	public static ChatMessageResponse toResponse(ChatMessage message) {
		return new ChatMessageResponse(
			message.getId(),
			message.getChatRoomId(),
			message.getSenderId(),
			message.getType(),
			message.getContent(),
			message.getImageUrl(),
			message.getImageOriginalName(),
			message.getReadAt(),
			message.getSentAt()
		);
	}
}
