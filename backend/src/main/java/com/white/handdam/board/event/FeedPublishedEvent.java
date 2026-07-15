package com.white.handdam.board.event;

/**
 * 유료 게시판 게시글이 등록되었을 때 발행하는 도메인 이벤트 (Phase 1: Spring Application Event).
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>TODO (NOTIFICATION 도메인 — 이번 범위 밖, 추후 구현):
 * <ul>
 *   <li>notification 패키지에 {@code FeedPublishedEvent} 구독 리스너 추가
 *       (예: {@code @Component} + {@code @EventListener} 또는 {@code @TransactionalEventListener(phase = AFTER_COMMIT)})</li>
 *   <li>리스너에서 이벤트 필드(postId, creatorId, writerId, title)로 알림 대상·메시지 구성</li>
 *   <li>알림 저장 (NOTIFICATION 테이블) — Flyway/스키마는 해당 작업에서 별도 처리</li>
 *   <li>알림 전송 (WebSocket / SSE 등) — Phase 1에서는 ApplicationEvent만 사용, Kafka/Redis는 Phase 2</li>
 * </ul>
 */
public record FeedPublishedEvent(
	Long postId,
	Long creatorId,
	Long writerId,
	String title
) {
}
