package com.white.handdam.creator.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 크리에이터·크리에이터 신청·카테고리·프로젝트 도메인 전용 에러 코드.
 */
@Getter
public enum CreatorErrorCode implements ErrorCode {

    // 크리에이터 신청
    APPLICATION_ALREADY_CREATOR(HttpStatus.CONFLICT, "이미 크리에이터입니다."),
    APPLICATION_PENDING_EXISTS(HttpStatus.CONFLICT, "이미 심사 중인 신청이 있습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "크리에이터 신청을 찾을 수 없습니다."),
    APPLICATION_NOT_PENDING(HttpStatus.CONFLICT, "심사 대기 중인 신청만 처리할 수 있습니다."),

    // 크리에이터 프로필
    CREATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "크리에이터를 찾을 수 없습니다."),

    // 공통 권한
    ADMIN_ONLY(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다.");

    private final HttpStatus status;
    private final String message;

    CreatorErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}