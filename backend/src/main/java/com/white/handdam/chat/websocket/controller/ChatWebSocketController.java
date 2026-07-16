package com.white.handdam.chat.websocket.controller;

import com.white.handdam.chat.websocket.dto.request.ChatWebSocketSendRequest;
import com.white.handdam.chat.websocket.dto.response.ChatWebSocketErrorResponse;
import com.white.handdam.chat.websocket.service.ChatWebSocketService;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.security.AuthMember;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

/**
 * STOMP 채팅 메시지 컨트롤러.
 *
 * <pre>
 * SEND  /pub/chat-rooms/{chatRoomId}/messages
 * → 구독자 /sub/chat-rooms/{chatRoomId} 로 브로드캐스트 (ChatMessagePublisher)
 *
 * 예외 발생 시 요청자 개인 큐 /user/queue/errors 로 에러 응답.
 * </pre>
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

	private final ChatWebSocketService chatWebSocketService;

	/**
	 * 텍스트 메시지 전송 (WebSocket).
	 *
	 * <p>클라이언트가 {@code /pub/chat-rooms/{chatRoomId}/messages} 로 SEND 하면 호출된다.
	 * 인증된 {@link AuthMember}를 꺼낸 뒤 {@link ChatWebSocketService}에 위임한다.
	 * 저장·브로드캐스트는 서비스/퍼블리셔 쪽에서 처리한다.
	 */
	@MessageMapping("/chat-rooms/{chatRoomId}/messages")
	public void sendMessage(
		@DestinationVariable Long chatRoomId,
		@Payload ChatWebSocketSendRequest request,
		Principal principal
	) {
		AuthMember member = requireAuthMember(principal);
		chatWebSocketService.sendMessage(chatRoomId, member.id(), request);
	}

	/**
	 * 도메인 {@link CustomException} 처리.
	 *
	 * 프론트는 에러 큐 하나만 구독하면 되고, code 값으로 어떤 오류인지 구분할 수 있다.
	 * 
	 * <p>권한·구독·방 상태 등 비즈니스 오류를 에러 코드/메시지로 변환해
	 * 요청자 개인 큐({@code /user/queue/errors})로 보낸다.
	 */
	@MessageExceptionHandler(CustomException.class)
	//요청자 개인 큐({@code /user/queue/errors})로 보낸다.
	@SendToUser("/queue/errors")
	public ChatWebSocketErrorResponse handleCustomException(CustomException ex) {
		return new ChatWebSocketErrorResponse(
			ex.getErrorCode().name(),
			ex.getErrorCode().getMessage()
		);
	}

	/**
	 * 예상치 못한 예외 처리.
	 *
	 * <p>{@link CustomException} 이외의 오류를 공통 코드 {@code CHAT_WEBSOCKET_ERROR}로 감싸
	 * 요청자 개인 큐({@code /user/queue/errors})로 보낸다.
	 */
	@MessageExceptionHandler(Exception.class)
	@SendToUser("/queue/errors")
	public ChatWebSocketErrorResponse handleException(Exception ex) {
		return new ChatWebSocketErrorResponse(
			"CHAT_WEBSOCKET_ERROR",
			ex.getMessage() == null ? "WebSocket 처리 중 오류가 발생했습니다." : ex.getMessage()
		);
	}

	/**
	 * STOMP {@link Principal}에서 {@link AuthMember}를 꺼낸다.
	 *
	 * <p>CONNECT 시 인터셉터가 JWT로 인증한
	 * {@link UsernamePasswordAuthenticationToken}을 기대한다.
	 * 인증 정보가 없거나 타입이 맞지 않으면 예외를 던진다.
	 * 
	 * REST의 @AuthenticationPrincipal AuthMember member와 같은 역할인데, WebSocket은 어노테이션이 없어서 직접 꺼냅니다.
	 */
	//STOMP {@link Principal}에서 {@link AuthMember}를 꺼낸다.
	private static AuthMember requireAuthMember(Principal principal) {
		//principal이 UsernamePasswordAuthenticationToken 타입이고, principal의 principal이 AuthMember 타입이면 AuthMember를 반환한다.
		if (principal instanceof UsernamePasswordAuthenticationToken token
			&& token.getPrincipal() instanceof AuthMember member) {
			return member;
		}
		throw new IllegalArgumentException("WebSocket 인증 정보가 없습니다.");
	}
}
