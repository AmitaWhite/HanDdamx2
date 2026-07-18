package com.white.handdam.chat.event;

/**
 * 채팅방이 새로 개설되었을 때 발행하는 도메인 이벤트 (Phase 1: Spring Application Event).
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>기존 방이 재사용된 경우에는 발행하지 않는다.
 * (CHAT-001 / LDJ-017 — {@code createOrGetChatRoom} 신규 INSERT 시점에만 발행)
 *
 * <p>TODO (NOTIFICATION 도메인 — 이번 범위 밖, 추후 구현):
 * <ul>
 *   <li>notification 패키지에 {@code ChatRoomCreatedEvent} 구독 리스너 추가
 *       (예: {@code @Component} + {@code @EventListener} 또는 {@code @TransactionalEventListener(phase = AFTER_COMMIT)})</li>
 *   <li>리스너에서 이벤트 필드(chatRoomId, creatorId, memberId)로 알림 대상·메시지 구성
 *       — 일반적으로 크리에이터에게 "구독자가 채팅을 시작했습니다" 알림</li>
 *   <li>알림 저장 (NOTIFICATION 테이블) — Flyway/스키마는 해당 작업에서 별도 처리</li>
 *   <li>알림 전송 (WebSocket / SSE 등) — Phase 1에서는 ApplicationEvent만 사용, Kafka/Redis는 Phase 2</li>
 * </ul>
 */
public record ChatRoomCreatedEvent(
	Long chatRoomId,
	Long creatorId,
	Long memberId
) {
}
