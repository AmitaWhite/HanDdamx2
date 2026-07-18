package com.white.handdam.chat.event;

import com.white.handdam.chat.entity.ChatMessageType;

/**
 * 채팅 메시지가 전송되었을 때 발행하는 도메인 이벤트 (Phase 1: Spring Application Event).
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>WebSocket 브로드캐스트(구독 중인 클라이언트 실시간 수신)와는 별개로,
 * 오프라인/미접속 사용자를 위한 알림 저장·전송을 위해 발행한다.
 * (CHAT-002~004 / LDJ-021 — {@code sendMessage} 저장 성공 후)
 *
 * <p>{@code recipientId} 는 채팅방 참여자 중 {@code senderId} 가 아닌 상대방으로,
 * 서비스에서 미리 계산해 전달한다(리스너 로직 단순화).
 *
 * <p>TODO (NOTIFICATION 도메인 — 이번 범위 밖, 추후 구현):
 * <ul>
 *   <li>notification 패키지에 {@code ChatMessageSentEvent} 구독 리스너 추가
 *       (예: {@code @Component} + {@code @EventListener} 또는 {@code @TransactionalEventListener(phase = AFTER_COMMIT)})</li>
 *   <li>리스너에서 이벤트 필드(chatRoomId, messageId, senderId, recipientId, type, contentPreview)로
 *       알림 대상·메시지 구성 — 대상은 {@code recipientId}</li>
 *   <li>알림 저장 (NOTIFICATION 테이블) — Flyway/스키마는 해당 작업에서 별도 처리</li>
 *   <li>알림 전송 (WebSocket / SSE / Push 등) — Phase 1에서는 ApplicationEvent만 사용, Kafka/Redis는 Phase 2</li>
 * </ul>
 *
 * @param contentPreview 알림 미리보기 텍스트. TEXT 는 트리밍된 본문(길이 제한은 리스너 책임),
 *                       IMAGE 는 {@code "[이미지]"} 등 고정 문구로 서비스에서 세팅한다.
 */
public record ChatMessageSentEvent(
	Long chatRoomId,
	Long messageId,
	Long senderId,
	Long recipientId,
	ChatMessageType type,
	String contentPreview
) {
}
