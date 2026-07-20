package com.white.handdam.notification.converter;

import com.white.handdam.notification.dto.response.NotificationResponse;
import com.white.handdam.notification.entity.NotificationEntity;

public final class NotificationConverter {

	private NotificationConverter() {
	}

	public static NotificationResponse toResponse(NotificationEntity notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getSenderId(),
				notification.getType(),
				notification.getMessage(),
				notification.getReferenceId(),
				notification.getReferenceType(),
				// isRead 는 Boolean 이라 언박싱 NPE 를 피해 null-safe 하게 읽는다
				Boolean.TRUE.equals(notification.getIsRead()),
				notification.getCreatedAt());
	}
}
