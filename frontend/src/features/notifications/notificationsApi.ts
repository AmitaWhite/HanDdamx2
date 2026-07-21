import { http, unwrap, unwrapVoid } from "@/lib/api";
import type { SliceResponse } from "@/lib/types";

/** 백엔드 NotificationEntity.NotificationType (com.white.handdam.notification.entity.NotificationEntity) */
export type NotificationType =
	| "NEW_FEED"
	| "FEED_COMMENT"
	| "FEED_REPLY"
	| "BOARD_ANSWER"
	| "BOARD_COMMENT"
	| "BOARD_REPLY"
	| "CHAT_MESSAGE"
	| "SUBSCRIPTION_EXPIRING"
	| "PAYMENT_SUCCESS"
	| "PAYMENT_FAILED"
	| "POLL_VOTE"
	| "CREATOR_APPLICATION_APPROVED"
	| "CREATOR_APPLICATION_REJECTED";

/** 백엔드 NotificationReferenceType (com.white.handdam.notification.entity.NotificationReferenceType) */
export type NotificationReferenceType =
	| "CHAT_ROOM"
	| "FEED"
	| "BOARD_POST"
	| "PAYMENT"
	| "CREATOR_APPLICATION"
	| "POLL";

/** 백엔드 NotificationResponse (com.white.handdam.notification.dto.response.NotificationResponse) */
export interface NotificationResponse {
	id: number;
	senderId: number | null;
	type: NotificationType;
	message: string;
	referenceId: number;
	referenceType: NotificationReferenceType;
	isRead: boolean;
	createdAt: string;
}

/** 백엔드 UnreadCountResponse */
export interface UnreadCountResponse {
	unreadCount: number;
}

/** 백엔드 NotificationReadAllResponse */
export interface NotificationReadAllResponse {
	updatedCount: number;
}

export interface GetMyNotificationsParams {
	page?: number;
	size?: number;
}

/**
 * 내 알림 목록 (최신순, 무한 스크롤).
 * 백엔드: GET /api/notifications
 */
export function getMyNotifications({ page = 0, size = 20 }: GetMyNotificationsParams = {}) {
	return unwrap<SliceResponse<NotificationResponse>>(
		http.get("/notifications", { params: { page, size } }),
	);
}

/**
 * 안 읽은 알림 개수 (뱃지).
 * 백엔드: GET /api/notifications/unread-count
 */
export function getUnreadCount() {
	return unwrap<UnreadCountResponse>(http.get("/notifications/unread-count"));
}

/**
 * 알림 읽음 처리 (단건).
 * 백엔드: PATCH /api/notifications/{notificationId}/read
 */
export function markNotificationRead(notificationId: number) {
	return unwrapVoid(http.patch(`/notifications/${notificationId}/read`));
}

/**
 * 내 알림 전체 읽음 처리.
 * 백엔드: PATCH /api/notifications/read-all
 */
export function markAllNotificationsRead() {
	return unwrap<NotificationReadAllResponse>(http.patch("/notifications/read-all"));
}

/** 알림 타입 → 아이콘. 헤더 드롭다운과 전체 알림 페이지가 공유. */
export const NOTIFICATION_TYPE_ICON: Record<NotificationType, string> = {
	NEW_FEED: "photo_library",
	FEED_COMMENT: "reply",
	FEED_REPLY: "reply",
	BOARD_ANSWER: "workspace_premium",
	BOARD_COMMENT: "reply",
	BOARD_REPLY: "reply",
	CHAT_MESSAGE: "chat_bubble",
	SUBSCRIPTION_EXPIRING: "schedule",
	PAYMENT_SUCCESS: "payments",
	PAYMENT_FAILED: "error",
	POLL_VOTE: "how_to_vote",
	CREATOR_APPLICATION_APPROVED: "workspace_premium",
	CREATOR_APPLICATION_REJECTED: "cancel",
};
