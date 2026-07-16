package com.white.handdam.auth.oauth;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum OAuthErrorCode implements ErrorCode {

    OAUTH_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 가입된 이메일입니다. 로컬 로그인을 이용해주세요."),
    OAUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "OAuth 로그인에 실패했습니다"),

    NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "OAuth 닉네임 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    OAuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
