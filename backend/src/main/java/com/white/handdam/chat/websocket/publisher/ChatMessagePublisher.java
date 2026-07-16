package com.white.handdam.chat.websocket.publisher;

import com.white.handdam.chat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 저장된 채팅 메시지를 구독 중인 클라이언트에게 브로드캐스트한다.
 */
@Component
@RequiredArgsConstructor
public class ChatMessagePublisher {

	//채팅방 구독 접두사
	public static final String ROOM_TOPIC_PREFIX = "/sub/chat-rooms/";

	//메시지 전송 템플릿
	private final SimpMessagingTemplate messagingTemplate;

	//채팅방 메시지 전송	chatRoomId: 채팅방 ID, message: 메시지 내용
	public void publish(Long chatRoomId, ChatMessageResponse message) {
		//채팅방 구독 접두사와 채팅방 ID를 결합하여 메시지 전송 경로를 생성한다.
		//메시지 전송 템플릿을 사용하여 메시지를 전송한다.
		messagingTemplate.convertAndSend(ROOM_TOPIC_PREFIX + chatRoomId, message);
	}
}
