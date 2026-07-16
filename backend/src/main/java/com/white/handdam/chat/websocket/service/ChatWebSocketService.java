package com.white.handdam.chat.websocket.service;

import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.entity.ChatMessageType;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.service.ChatMessageService;
import com.white.handdam.chat.websocket.dto.request.ChatWebSocketSendRequest;
import com.white.handdam.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * WebSocket 채팅 전송 유스케이스.
 * 저장·권한은 {@link ChatMessageService}에 위임하고, 브로드캐스트는 서비스 저장 후 발행된다.
 */
@Service
@RequiredArgsConstructor
public class ChatWebSocketService {

	private final ChatMessageService chatMessageService;

	public ChatMessageResponse sendMessage(
		Long chatRoomId,
		Long senderId,
		ChatWebSocketSendRequest request
	) {
		if (request == null || request.type() == null) {
			throw new CustomException(ChatErrorCode.CHAT_MESSAGE_INVALID);
		}
		// 바이너리 업로드는 REST multipart 담당
		if (request.type() != ChatMessageType.TEXT) {
			throw new CustomException(ChatErrorCode.CHAT_MESSAGE_INVALID);
		}

		return chatMessageService.sendMessage(
			chatRoomId,
			senderId,
			ChatMessageType.TEXT,
			request.content(),
			null
		);
	}
}
