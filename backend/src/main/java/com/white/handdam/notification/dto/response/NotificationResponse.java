package com.white.handdam.notification.dto.response;

import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import java.time.Instant;

/**
 * 알림 응답.
 *
 * 목록 조회와 WebSocket 실시간 전송에 동일하게 사용한다.
 * 프론트가 목록에 그리는 카드와 실시간으로 받아 끼워 넣는 카드가 같아야 하기 때문이다.
 *
 * {@code referenceType}/{@code referenceId} 로 클릭 시 이동 대상을 결정한다.
 * (예: {@code CHAT_ROOM} + {@code 42} → 채팅방 42번)
 */
public record NotificationResponse(
		Long id,
		Long senderId,
		NotificationType type,
		String message,
		Long referenceId,
		String referenceType,
		boolean isRead,
		Instant createdAt) {
}
