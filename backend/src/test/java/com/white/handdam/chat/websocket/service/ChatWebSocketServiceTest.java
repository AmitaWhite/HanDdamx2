package com.white.handdam.chat.websocket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.entity.ChatMessageType;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.service.ChatMessageService;
import com.white.handdam.chat.websocket.dto.request.ChatWebSocketSendRequest;
import com.white.handdam.global.exception.CustomException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketServiceTest {

	@Mock
	private ChatMessageService chatMessageService;

	@InjectMocks
	private ChatWebSocketService chatWebSocketService;

	@Test
	@DisplayName("WebSocket으로 텍스트 메시지를 전송할 수 있다")
	void sendTextMessage() {
		ChatMessageResponse saved = new ChatMessageResponse(
			1L, 10L, 99L, ChatMessageType.TEXT, "안녕", null, null, null,
			Instant.parse("2026-07-17T00:00:00Z")
		);
		given(chatMessageService.sendMessage(10L, 99L, ChatMessageType.TEXT, "안녕", null))
			.willReturn(saved);

		ChatMessageResponse result = chatWebSocketService.sendMessage(
			10L,
			99L,
			new ChatWebSocketSendRequest(ChatMessageType.TEXT, "안녕")
		);

		assertThat(result.content()).isEqualTo("안녕");
		verify(chatMessageService).sendMessage(10L, 99L, ChatMessageType.TEXT, "안녕", null);
	}

	@Test
	@DisplayName("WebSocket에서는 IMAGE 타입을 보낼 수 없다")
	void rejectImageType() {
		assertThatThrownBy(() -> chatWebSocketService.sendMessage(
			10L,
			99L,
			new ChatWebSocketSendRequest(ChatMessageType.IMAGE, null)
		)).satisfies(ex -> {
			assertThat(ex).isInstanceOf(CustomException.class);
			assertThat(((CustomException) ex).getErrorCode())
				.isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID);
		});

		verify(chatMessageService, never()).sendMessage(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	@DisplayName("요청이 없으면 INVALID")
	void nullRequestInvalid() {
		assertThatThrownBy(() -> chatWebSocketService.sendMessage(10L, 99L, null))
			.satisfies(ex -> assertThat(((CustomException) ex).getErrorCode())
				.isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID));
	}
}
