package com.white.handdam.chat.websocket.interceptor;

import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.service.ChatRoomService;
import com.white.handdam.chat.websocket.publisher.ChatMessagePublisher;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.global.security.jwt.JwtTokenProvider;
import com.white.handdam.notification.exception.NotificationErrorCode;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP 인바운드 채널 인증·구독 권한 인터셉터.
 *
 * <p>
 * {@link com.white.handdam.chat.websocket.config.ChatWebSocketConfig#configureClientInboundChannel}
 * 에 등록되어,
 * 클라이언트가 보내는 STOMP 프레임이 컨트롤러에 도달하기 전에 가로챈다.
 *
 * <ul>
 * <li>{@link StompCommand#CONNECT} — JWT 검증 후 {@code AuthMember}를 Principal로
 * 세팅</li>
 * <li>{@link StompCommand#SUBSCRIBE} — {@code /sub/chat-rooms/{id}} 구독 시 참여자
 * 검사</li>
 * </ul>
 *
 * <p>
 * 토큰 조회 우선순위 ({@link #resolveToken}):
 * <ol>
 * <li>native header {@code Authorization: Bearer ...}</li>
 * <li>native header {@code token}</li>
 * <li>handshake 세션 attribute {@code accessToken}</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	/** JWT 발급·검증·Principal 생성 */
	private final JwtTokenProvider jwtTokenProvider;

	/** 채팅방 참여자 검사 ({@link ChatRoomService#requireParticipatingRoom}) */
	private final ChatRoomService chatRoomService;

	/**
	 * 클라이언트 → 서버 STOMP 프레임이 지나가기 직전에 호출된다.
	 *
	 * <p>
	 * 명령별 처리:
	 * <ul>
	 * <li>CONNECT — 로그인(JWT → AuthMember)</li>
	 * <li>SUBSCRIBE — 채팅방 토픽이면 참여자 여부 검사</li>
	 * <li>SEND, DISCONNECT 등 — 이 인터셉터에서는 통과 (권한은 서비스/컨트롤러)</li>
	 * </ul>
	 *
	 * @param message STOMP 프레임 (CONNECT, SUBSCRIBE, SEND …)
	 * @param channel 인바운드 메시지 채널
	 * @return 검사 통과 후 그대로 넘길 메시지 (에러 시 예외 throw)
	 */
	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		// STOMP 헤더·명령·user·destination 등을 읽기 위한 접근자
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
				message,
				StompHeaderAccessor.class);
		if (accessor == null) {
			return message;
		}
		/**
		 * JWT 가져오기
		 * → JWT 유효성 검사
		 * → Authentication 생성 (UsernamePasswordAuthenticationToken)
		 * → Principal 등록 (accessor.setUser(authentication))
		 */
		if (accessor.getCommand() == StompCommand.CONNECT) {
			handleConnect(accessor);
			// 채팅방 토픽 구독 시 참여자(creator 또는 member)만 허용한다.
			// 알림 토픽 구독 시 본인만 허용한다.
		} else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
			assertCanSubscribeRoom(accessor);
			assertCanSubscribeNotification(accessor);
		}
		return message;
	}

	/**
	 * STOMP CONNECT — WebSocket 세션에 로그인 유저를 심는다.
	 *
	 * <p>
	 * 이후 SUBSCRIBE/SEND에서 {@code accessor.getUser()} / 컨트롤러 {@code Principal}로
	 * {@link AuthMember}를 꺼낼 수 있다.
	 *
	 * @throws IllegalArgumentException 토큰 없음·무효 시 CONNECT 거부
	 */
	private void handleConnect(StompHeaderAccessor accessor) {
		String token = resolveToken(accessor);
		if (!StringUtils.hasText(token) || !jwtTokenProvider.validate(token)) {
			throw new IllegalArgumentException("WebSocket 인증에 실패했습니다. 유효한 JWT가 필요합니다.");
		}
		Authentication authentication = jwtTokenProvider.getAuthentication(token);
		accessor.setUser(authentication);
	}

	/**
	 * 채팅방 토픽 구독 시 참여자(creator 또는 member)만 허용한다.
	 *
	 * <p>
	 * {@code /sub/chat-rooms/{chatRoomId}} 형태일 때만 검사한다.
	 * {@code /user/queue/errors} 같은 개인 큐·다른 목적지는 검사하지 않는다.
	 *
	 * @throws CustomException 로그인 안 됨({@link ChatErrorCode#CHAT_LOGIN_REQUIRED}),
	 *                         참여자 아님({@link ChatErrorCode#CHAT_NOT_PARTICIPANT}),
	 *                         방 없음({@link ChatErrorCode#CHAT_ROOM_NOT_FOUND})
	 */
	private void assertCanSubscribeRoom(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		// 채팅방 브로드캐스트 토픽이 아니면 통과 (에러 큐 등)
		// destination이 null, 빈 문자열, 공백만 있는 경우
		if (!StringUtils.hasText(destination)
				// 채팅방 토픽 prefix가 아닌 경우
				|| !destination.startsWith(ChatMessagePublisher.ROOM_TOPIC_PREFIX)) {
			return;
		}

		Long chatRoomId = parseChatRoomId(destination);
		AuthMember member = requireAuthMember(accessor.getUser());
		chatRoomService.requireParticipatingRoom(chatRoomId, member.id());
	}

	/**
	 * 알림 토픽 구독 시 본인만 허용한다.
	 *
	 * <p>
	 * {@code /sub/notifications/{memberId}} 형태일 때만 검사한다.
	 * 이 검사가 없으면 인증된 아무나 남의 memberId로 구독해 타인의 알림 스트림을 수신할 수 있다.
	 *
	 * <p>
	 * 채팅방과 달리 소유권이 단순 ID 비교라 DB 조회가 필요 없다.
	 *
	 * @throws CustomException 로그인 안 됨({@link ChatErrorCode#CHAT_LOGIN_REQUIRED}),
	 *                         본인 아님·형식
	 *                         오류({@link NotificationErrorCode#NOTIFICATION_FORBIDDEN})
	 */
	private void assertCanSubscribeNotification(StompHeaderAccessor accessor) {
		String destination = accessor.getDestination();
		// 알림 개인 토픽이 아니면 통과
		if (!StringUtils.hasText(destination)
				|| !destination.startsWith(NotificationPublisher.NOTIFICATION_TOPIC_PREFIX)) {
			return;
		}

		Long targetMemberId = parseNotificationMemberId(destination);
		AuthMember member = requireAuthMember(accessor.getUser());
		if (!member.id().equals(targetMemberId)) {
			throw new CustomException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
		}
	}

	/**
	 * {@code /sub/notifications/{memberId}} destination에서 회원 ID를 추출한다.
	 *
	 * <p>
	 * {@link #parseChatRoomId} 와 동일한 가드를 적용한다 —
	 * 값이 없거나 뒤에 경로가 더 붙은 경우({@code /sub/notifications/1/2})는 거부한다.
	 *
	 * @throws CustomException ID 없음·형식 오류·숫자 아님 →
	 *                         {@link NotificationErrorCode#NOTIFICATION_FORBIDDEN}
	 */
	private static Long parseNotificationMemberId(String destination) {
		String memberIdPart = destination.substring(NotificationPublisher.NOTIFICATION_TOPIC_PREFIX.length());
		if (!StringUtils.hasText(memberIdPart) || memberIdPart.contains("/")) {
			throw new CustomException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
		}
		try {
			return Long.valueOf(memberIdPart);
		} catch (NumberFormatException ex) {
			throw new CustomException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
		}
	}

	/**
	 * {@code /sub/chat-rooms/{id}} destination에서 채팅방 ID를 추출한다.
	 *
	 * <p>
	 * 예: {@code /sub/chat-rooms/42} → {@code 42L}
	 *
	 * @throws CustomException ID 없음·형식 오류·숫자 아님 →
	 *                         {@link ChatErrorCode#CHAT_ROOM_NOT_FOUND}
	 */
	private static Long parseChatRoomId(String destination) {
		// "/sub/chat-rooms/" 이후 문자열이 room id
		// destination이 "/sub/chat-rooms/42"라면, prefix 길이만큼 앞부분을 잘라서 "42"만 남깁니다.
		String roomIdPart = destination.substring(ChatMessagePublisher.ROOM_TOPIC_PREFIX.length());
		// 즉 ID가 아예 없거나 공백뿐인 경우,뒤에 경로가 더 붙은 경우 예외 발생
		if (!StringUtils.hasText(roomIdPart) || roomIdPart.contains("/")) {
			throw new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
		}
		try {
			// roomIdPart를 Long 타입으로 변환
			return Long.valueOf(roomIdPart);
		} catch (NumberFormatException ex) {
			throw new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
		}
	}

	/**
	 * STOMP 세션 {@link Principal}에서 {@link AuthMember}를 꺼낸다.
	 *
	 * <p>
	 * CONNECT 시 {@link #handleConnect}가
	 * {@link UsernamePasswordAuthenticationToken}으로
	 * principal에 {@code AuthMember}를 넣어 둔 것을 기대한다.
	 *
	 * @throws CustomException CONNECT 전이거나 인증 정보 없음 →
	 *                         {@link ChatErrorCode#CHAT_LOGIN_REQUIRED}
	 */
	private static AuthMember requireAuthMember(Principal principal) {
		if (principal instanceof UsernamePasswordAuthenticationToken token
				&& token.getPrincipal() instanceof AuthMember member) {
			return member;
		}
		throw new CustomException(ChatErrorCode.CHAT_LOGIN_REQUIRED);
	}

	/**
	 * CONNECT 프레임에서 JWT 문자열을 꺼낸다.
	 *
	 * <p>
	 * 클라이언트 환경마다 토큰 전달 방식이 달라 우선순위로 찾는다.
	 * 앞 단계에서 값이 나오면 즉시 반환한다.
	 *
	 * <ol>
	 * <li>{@code Authorization: Bearer &lt;jwt&gt;}</li>
	 * <li>{@code token: &lt;jwt&gt;}</li>
	 * <li>핸드셰이크 세션 {@code accessToken} ({@link JwtHandshakeInterceptor})</li>
	 * </ol>
	 *
	 * @return JWT 문자열, 없으면 {@code null}
	 */
	private String resolveToken(StompHeaderAccessor accessor) {
		// 1순위: Authorization: Bearer eyJ...
		String authorization = firstHeader(accessor, "Authorization");
		if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
			return authorization.substring(7).trim();
		}

		// 2순위: token: eyJ...
		String tokenHeader = firstHeader(accessor, "token");
		if (StringUtils.hasText(tokenHeader)) {
			return tokenHeader.trim();
		}

		// 3순위: /ws?access_token=... 로 핸드셰이크 때 저장한 값
		Object handshakeToken = accessor.getSessionAttributes() == null
				? null
				: accessor.getSessionAttributes().get(JwtHandshakeInterceptor.ATTR_ACCESS_TOKEN);
		return handshakeToken == null ? null : handshakeToken.toString();
	}

	/**
	 * STOMP native header의 첫 번째 값을 반환한다.
	 * 같은 이름의 헤더가 여러 개일 수 있어 첫 값만 사용한다.
	 *
	 * @return 헤더 값, 없으면 {@code null}
	 */
	private static String firstHeader(StompHeaderAccessor accessor, String name) {
		List<String> values = accessor.getNativeHeader(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.getFirst();
	}
}
