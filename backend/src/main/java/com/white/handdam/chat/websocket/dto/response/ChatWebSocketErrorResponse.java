package com.white.handdam.chat.websocket.dto.response;

public record ChatWebSocketErrorResponse(
	String code,
	String message
) {
}
