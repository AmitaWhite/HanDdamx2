package com.white.handdam.chat.converter;

import com.white.handdam.chat.dto.response.ChatMessagePreviewResponse;
import com.white.handdam.chat.dto.response.ChatRoomListItemResponse;
import com.white.handdam.chat.dto.response.ChatRoomResponse;
import com.white.handdam.chat.entity.ChatMessage;
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

	public static ChatRoomListItemResponse toListItem(
		ChatRoom room,
		ChatMessage lastMessage,
		long unreadCount
	) {
		return new ChatRoomListItemResponse(
			room.getId(),
			room.getCreatorId(),
			room.getMemberId(),
			room.getStatus(),
			room.getClosedBy(),
			room.getClosedAt(),
			room.getLastMessageAt(),
			room.getCreatedAt(),
			room.getUpdatedAt(),
			lastMessage == null ? null : toMessagePreview(lastMessage),
			unreadCount
		);
	}

	public static ChatMessagePreviewResponse toMessagePreview(ChatMessage message) {
		return new ChatMessagePreviewResponse(
			message.getId(),
			message.getSenderId(),
			message.getType(),
			message.getContent(),
			message.getImageUrl(),
			message.getSentAt(),
			message.getReadAt()
		);
	}
}
