package com.white.handdam.chat.websocket.dto.request;

import com.white.handdam.chat.entity.ChatMessageType;

/**
 * WebSocket 텍스트 메시지 전송 요청.
 * 이미지 업로드는 multipart REST({@code POST /api/chat-rooms/{id}/messages})를 사용한다.
 */
public record ChatWebSocketSendRequest(
	ChatMessageType type,
	String content
) {
}
