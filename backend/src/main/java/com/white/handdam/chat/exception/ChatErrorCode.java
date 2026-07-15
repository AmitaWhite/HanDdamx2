package com.white.handdam.chat.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ChatErrorCode implements ErrorCode {

	CHAT_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	CHAT_SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "유료 구독자만 채팅방을 생성할 수 있습니다."),
	CHAT_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신과는 채팅할 수 없습니다."),
	CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다."),
	CHAT_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 채팅방의 참여자만 조회할 수 있습니다.");

	private final HttpStatus status;
	private final String message;

	ChatErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
}
