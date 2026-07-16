package com.white.handdam.chat.websocket.config;

import com.white.handdam.chat.websocket.interceptor.JwtHandshakeInterceptor;
import com.white.handdam.chat.websocket.interceptor.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 채팅 STOMP WebSocket 설정.
 *
 * <p>비즈니스 로직은 없고, 클라이언트가 연결·송신·구독할 주소와 인증 인터셉터만 정의한다.
 *
 * <pre>
 * 연결:   /ws (SockJS)  — JWT 핸드셰이크
 * 송신:   /pub/chat-rooms/{chatRoomId}/messages  → ChatWebSocketController
 * 구독:   /sub/chat-rooms/{chatRoomId}           ← ChatMessagePublisher 브로드캐스트
 * 에러:   /user/queue/errors                     ← MessageExceptionHandler
 * </pre>
 *
 * <p>{@link EnableWebSocketMessageBroker} 로 STOMP over WebSocket 메시지 브로커를 켠다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {

	/** CONNECT/SEND 등 STOMP 프레임마다 JWT·Principal을 검증·주입한다. */
	private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

	/** HTTP → WebSocket 업그레이드(핸드셰이크) 시점에 JWT를 검사한다. */
	private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

	/**
	 * CORS 허용 origin.
	 * application.yml {@code app.frontend-base-url} 우선, 없으면 로컬 프론트 기본값.
	 */
	@Value("${app.frontend-base-url:http://localhost:3000}")
	private String frontendBaseUrl;

	/**
	 * STOMP 엔드포인트 등록.
	 *
	 * <ul>
	 *   <li>{@code /ws} — 클라이언트가 SockJS로 접속하는 주소</li>
	 *   <li>{@link JwtHandshakeInterceptor} — 핸드셰이크 시 JWT 유효성 검사</li>
	 *   <li>{@code setAllowedOrigins} — 프론트 origin만 허용 (CORS)</li>
	 *   <li>{@code withSockJS()} — 브라우저 WebSocket 미지원 환경 폴백</li>
	 * </ul>
	 */
	@Override
	//클라이언트가 웹소켓 연결을 시작할 주소를 등록한다.
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
			.addInterceptors(jwtHandshakeInterceptor)
			.setAllowedOrigins(frontendBaseUrl)
			//SockJS를 사용하여 브라우저의 WebSocket 미지원 환경을 폴백한다.
			.withSockJS();
	}

	/**
	 * 메시지 브로커·목적지 prefix 설정.
	 *
	 * <ul>
	 *   <li>{@code /sub}, {@code /queue} — 서버 → 클라이언트 구독 경로
	 *       (방 브로드캐스트는 /sub, 개인 에러 큐는 /queue)</li>
	 *   <li>{@code /pub} — 클라이언트 → 서버 SEND 경로.
	 *       실제 매핑은 {@code @MessageMapping} (앞에 /pub 붙음)</li>
	 *   <li>{@code /user} — 유저별 목적지 prefix.
	 *       {@code @SendToUser("/queue/errors")} → {@code /user/queue/errors}</li>
	 * </ul>
	 * | prefix   | 메시지 방향        | 처리하는 곳                   | 용도              |
	 * | -------- | ------------- | ------------------------ | --------------- |
	 * | `/pub`   | 클라이언트 → 서버    | `@MessageMapping`        | 메시지 전송, 읽음 처리 등 |
	 * | `/sub`   | 서버 → 여러 클라이언트 | Simple Broker            | 채팅방 메시지 브로드캐스트  |
	 * | `/queue` | 서버 → 클라이언트    | Simple Broker            | 개인 응답·에러 경로로 사용 |
	 * | `/user`  | 서버 → 특정 사용자   | User Destination Handler | 사용자별 메시지 구분     |
	 */
	@Override
	//메시지 경로 규칙과 메시지 브로커를 설정하는 메서드
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// 서버 → 클라이언트 구독 prefix ///sub 채팅방 전체 구독용 ///queue 개인 메시지용으로 사용
		registry.enableSimpleBroker("/sub", "/queue");
		// 클라이언트 → 서버 송신 prefix ///pub 채팅방 메시지 전송용
		registry.setApplicationDestinationPrefixes("/pub");
		// 특정 유저 대상 메시지 prefix (/user/queue/errors 등) ///user 개인 메시지용으로 사용
		registry.setUserDestinationPrefix("/user");
	}

	/**
	 * 클라이언트 → 서버 인바운드 채널에 STOMP 인증 인터셉터 등록.
	 *
	 * <p>핸드셰이크 이후에도 CONNECT/SEND 프레임마다
	 * {@link StompAuthChannelInterceptor}가 JWT를 확인하고
	 * {@code Principal}에 {@code AuthMember}를 심는다.
	 * 컨트롤러의 {@code Principal} 파라미터가 여기서 채워진다.
	 */
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(stompAuthChannelInterceptor);
	}
}
