package com.white.handdam.notification.entity;

/**
 * 알림이 가리키는 대상의 종류 (다형 참조).
 *
 * <p>{@code referenceId} 와 짝을 이뤄 프론트가 알림 클릭 시 이동할 화면을 결정한다.
 * (예: {@code BOARD_POST} + {@code 42} → 게시글 42번)
 *
 * <p>DB 의 {@code notification.reference_type} 은 {@code VARCHAR(50)} 이고 CHECK 제약이 없다.
 * 저장은 {@link NotificationEntity} 가 문자열로 하되, 알림을 만드는 쪽은 이 enum 을 쓰게 해
 * 문자열 표류({@code BOARD_POST} / {@code BOARDPOST} / {@code board_post})를 컴파일 단계에서 막는다.
 *
 * <p>값을 추가·변경하면 프론트 라우팅과 계약이 깨지므로 프론트와 함께 확인할 것.
 */
public enum NotificationReferenceType {

	/** 채팅방 — 채팅 메시지 알림 */
	CHAT_ROOM,

	/** 피드 — 새 피드, 피드 댓글·대댓글 알림 (댓글도 피드 화면으로 이동) */
	FEED,

	/** 유료 게시글 — 게시판 댓글·대댓글·공식 답변 알림 (모두 게시글 화면으로 이동) */
	BOARD_POST,

	/** 결제 — 결제 성공·실패 알림 */
	PAYMENT,

	/** 크리에이터 전환 신청 — 심사 승인·거절 알림 */
	CREATOR_APPLICATION,

	/** 투표 — 투표 참여 알림 */
	POLL
}
