package com.white.handdam.chat.converter;

import com.white.handdam.chat.dto.response.ChatRoomResponse;
import com.white.handdam.chat.entity.ChatRoom;

public final class ChatRoomConverter {

	private ChatRoomConverter() {
	}

	public static ChatRoomResponse toResponse(ChatRoom room) {
		return new ChatRoomResponse(
			room.getId(),
			room.getCreatorId(),
			room.getMemberId(),
			room.getStatus(),
			room.getClosedBy(),
			room.getClosedAt(),
			room.getLastMessageAt(),
			room.getCreatedAt(),
			room.getUpdatedAt()
		);
	}
}
