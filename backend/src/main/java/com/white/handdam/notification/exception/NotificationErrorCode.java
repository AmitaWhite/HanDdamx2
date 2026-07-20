package com.white.handdam.notification.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NotificationErrorCode implements ErrorCode {

	NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
	NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 알림만 처리할 수 있습니다.");

	private final HttpStatus status;
	private final String message;

	NotificationErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
}
