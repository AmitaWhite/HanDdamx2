package com.white.handdam.chat.websocket.interceptor;

import com.white.handdam.global.security.jwt.JwtTokenProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * HTTP → WebSocket 핸드셰이크 단계에서 JWT를 받는 인터셉터.
 *
 * <p>브라우저 WebSocket/SockJS는 커스텀 HTTP 헤더를 넣기 어려운 경우가 많다.
 * 그래서 클라이언트가 {@code /ws?access_token=...} 처럼 쿼리로 토큰을 넘기면,
 * 여기서 검증한 뒤 WebSocket 세션 attribute에 저장한다.
 *
 * <p>이후 STOMP CONNECT 때 {@link StompAuthChannelInterceptor}가
 * 이 attribute를 꺼내 Principal({@code AuthMember})을 세팅한다.
 *
1. JwtHandshakeInterceptor가 access_token을 꺼냄
2. JWT가 유효한지 검사
3. 유효하면 웹소켓 세션에 토큰 저장
4. 이후 STOMP CONNECT가 들어옴
5. StompAuthChannelInterceptor가 저장된 토큰을 꺼냄
6. JWT에서 회원 정보를 찾아 Principal로 설정
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

	/** 세션 attribute 키. STOMP 쪽에서 같은 키로 조회한다. */
	public static final String ATTR_ACCESS_TOKEN = "accessToken";

	private final JwtTokenProvider jwtTokenProvider;

	/**
	 * 핸드셰이크 직전 호출.
	 *
	 * <ol>
	 *   <li>Servlet 요청이 아니면 그대로 통과</li>
	 *   <li>쿼리 {@code access_token} 이 없으면 통과 (CONNECT에서 다시 검사)</li>
	 *   <li>토큰이 있으면 {@link JwtTokenProvider#validate} — 실패 시 handshake 거부</li>
	 *   <li>성공 시 {@link #ATTR_ACCESS_TOKEN} 으로 세션 attribute에 저장</li>
	 * </ol>
	 *
	 * @return {@code true} 핸드셰이크 계속, {@code false} 연결 거부
	 */
	@Override
	public boolean beforeHandshake(
		ServerHttpRequest request,
		ServerHttpResponse response,
		WebSocketHandler wsHandler,
		Map<String, Object> attributes
	) {
		//현재 요청이 서블릿 기반 HTTP 요청이 아닌가?
		//Servlet 요청이 아님
		//이 단계에서는 검사하지 않음
		//STOMP CONNECT 단계에서 다시 인증
		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			return true;
		}
		String token = servletRequest.getServletRequest().getParameter("access_token");
		if (!StringUtils.hasText(token)) {
			return true;
		}
		if (!jwtTokenProvider.validate(token)) {
			return false;
		}
		attributes.put(ATTR_ACCESS_TOKEN, token);
		return true;
	}

	/**
	 * 핸드셰이크 완료 후 콜백. 추가 처리 없음.
	 */
	@Override
	public void afterHandshake(
		ServerHttpRequest request,
		ServerHttpResponse response,
		WebSocketHandler wsHandler,
		Exception exception
	) {
		// no-op
	}
}
