package com.white.handdam.notification.websocket.publisher;

import com.white.handdam.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 저장된 알림을 수신자에게 실시간 전송한다.
 *
 * <p>목적지는 사용자별 개인 토픽 {@code /sub/notifications/{memberId}} 이며,
 * 구독 권한 검사는
 * {@link com.white.handdam.chat.websocket.interceptor.StompAuthChannelInterceptor} 가 담당한다.
 *
 * <p>{@code convertAndSendToUser} 를 쓰지 않는 이유:
 * {@code AuthMember} 가 record 라 {@code Principal.getName()} 이
 * {@code "1"} 이 아니라 {@code "AuthMember[id=1, role=USER]"} 를 반환한다.
 * 사용자 목적지 라우팅이 조용히 실패하므로 개인 토픽 방식을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

	/** 알림 개인 토픽 접두사 */
	public static final String NOTIFICATION_TOPIC_PREFIX = "/sub/notifications/";

	private final SimpMessagingTemplate messagingTemplate;

	public void publish(Long memberId, NotificationResponse notification) {
		messagingTemplate.convertAndSend(NOTIFICATION_TOPIC_PREFIX + memberId, notification);
	}
}
