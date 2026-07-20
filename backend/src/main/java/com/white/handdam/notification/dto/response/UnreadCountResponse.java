package com.white.handdam.notification.dto.response;

/**
 * 안 읽은 알림 개수 (뱃지용).
 *
 * 스칼라를 그대로 반환하지 않고 감싸서 JSON 형태를 안정적으로 유지한다.
 */
public record UnreadCountResponse(
        long unreadCount) {
}
