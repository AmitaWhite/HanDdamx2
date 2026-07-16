package com.white.handdam.chat.websocket.interceptor;

import com.white.handdam.global.security.jwt.JwtTokenProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP 인바운드 채널 인증 인터셉터.
 *
 * <p>{@link com.white.handdam.chat.websocket.config.ChatWebSocketConfig#configureClientInboundChannel} 에 등록되어,
 * 클라이언트가 보내는 STOMP 프레임이 컨트롤러에 도달하기 전에 가로챈다.
 *
 * <p>현재는 {@link StompCommand#CONNECT} 만 처리한다.
 * CONNECT 시 JWT를 검증하고 {@code accessor.setUser(authentication)} 으로
 * Principal({@code AuthMember})을 세션에 심는다.
 * 이후 SEND 등에서 컨트롤러의 {@code Principal} 파라미터가 이 값을 받는다.
 *
 * <p>토큰 조회 우선순위 ({@link #resolveToken}):
 * <ol>
 *   <li>native header {@code Authorization: Bearer ...}</li>
 *   <li>native header {@code token}</li>
 *   <li>handshake 세션 attribute {@code accessToken}
 *       ({@link JwtHandshakeInterceptor}가 쿼리 {@code access_token}으로 저장)</li>
 * </ol>
 *
 * <p>토큰이 없거나 무효하면 CONNECT를 거부한다 (IllegalArgumentException).
 * 브라우저 SockJS는 보통 3번 경로, 네이티브 클라이언트는 1·2번 헤더를 쓸 수 있다.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

	private final JwtTokenProvider jwtTokenProvider;

	/**
	 * 메시지 전송 직전 훅.
	 *
	 * <p>CONNECT가 아니면 그대로 통과한다.
	 * CONNECT이면 토큰을 찾아 검증하고, Authentication을 user로 세팅한다.
	 *
	 * @throws IllegalArgumentException 토큰 없음/무효 시 CONNECT 거부
	 */
	@Override
	//메시지 전송 직전 훅
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		//메시지 헤더 접근자 가져오기
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
			message,
			StompHeaderAccessor.class
		);
		//메시지 헤더 접근자가 없거나 명령이 CONNECT가 아니면 통과
		if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
			return message;
		}

		//토큰 조회
		String token = resolveToken(accessor);
		//토큰이 없거나, 토큰이 유효하지 않다면 연결을 거부한다.
		if (!StringUtils.hasText(token) || !jwtTokenProvider.validate(token)) {
			throw new IllegalArgumentException("WebSocket 인증에 실패했습니다. 유효한 JWT가 필요합니다.");
		}
		//토큰을 사용하여 인증 정보를 가져온다.
		Authentication authentication = jwtTokenProvider.getAuthentication(token);
		//인증 정보를 메시지 헤더 접근자에 설정한다.
		accessor.setUser(authentication);
		return message;
	}

	/**
	 * CONNECT 프레임에서 JWT 문자열을 꺼낸다.
	 *
	 * <p>클라이언트/환경마다 토큰을 넣는 방식이 달라서, 아래 순서로 찾는다.
	 * 앞 단계에서 유효한 값이 나오면 즉시 반환하고 뒤는 보지 않는다.
	 *
	 * <ol>
	 *   <li>{@code Authorization: Bearer &lt;jwt&gt;} — 네이티브/일반 STOMP 클라이언트</li>
	 *   <li>{@code token: &lt;jwt&gt;} — Bearer 없이 토큰만 보내는 경우</li>
	 *   <li>핸드셰이크 세션 attribute {@code accessToken}
	 *       — 브라우저 SockJS가 {@code /ws?access_token=...} 로 넘긴 값
	 *       ({@link JwtHandshakeInterceptor}가 저장)</li>
	 * </ol>
	 *
	 * @return JWT 문자열, 세 경로 모두 없으면 {@code null}
	 */
	private String resolveToken(StompHeaderAccessor accessor) {
		// 1순위: Authorization 헤더 (예: "Bearer eyJhbGciOi...")
		String authorization = firstHeader(accessor, "Authorization");
		//Bearer는 “이 토큰을 가진 사람이 접근해도 된다”는 인증 방식 이름
		if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
			// "Bearer " (7글자) 뒤의 JWT만 추출
			return authorization.substring(7).trim();
		}

		// 2순위: token 헤더 (Bearer 접두사 없이 JWT 본문만)
		//Authorization이 없거나 Bearer 형식이 아닐 때
		//token: eyJ...처럼 JWT만 보낸 경우 그대로 사용
		String tokenHeader = firstHeader(accessor, "token");
		if (StringUtils.hasText(tokenHeader)) {
			return tokenHeader.trim();
		}

		// 3순위: HTTP 핸드셰이크 때 세션에 넣어 둔 access_token
		// 세션 attribute 맵이 없으면 토큰도 없음
		//션 정보가 없으면 → null
		//있으면 → accessToken 키로 저장된 값 조회
		Object handshakeToken = accessor.getSessionAttributes() == null
			? null
			: accessor.getSessionAttributes().get(JwtHandshakeInterceptor.ATTR_ACCESS_TOKEN);
		// attribute에 저장된 객체를 문자열 JWT로 변환 (없으면 null)
		return handshakeToken == null ? null : handshakeToken.toString();
	}

	/**
	 * STOMP native header의 첫 번째 값을 반환한다.
	 * 헤더가 없거나 비어 있으면 {@code null}.
	 */
	private static String firstHeader(StompHeaderAccessor accessor, String name) {
		List<String> values = accessor.getNativeHeader(name);
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.getFirst();
	}
}
