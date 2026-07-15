package com.white.handdam.auth.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthErrorCode implements ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 토큰입니다."),
    EXPIRED_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, "만료된 인증 토큰입니다."),
    ALREADY_PROCESSED_VERIFICATION(HttpStatus.BAD_REQUEST, "이미 처리된 인증 요청입니다."),

    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
