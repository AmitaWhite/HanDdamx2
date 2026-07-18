package com.white.handdam.poll.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PollErrorCode implements ErrorCode {
    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."),
    POLL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이 피드에 이미 투표가 존재합니다."),
    POLL_CLOSED(HttpStatus.BAD_REQUEST, "이미 종료된 투표입니다."),
    POLL_FORBIDDEN(HttpStatus.FORBIDDEN, "투표에 대한 권한이 없습니다."),
    POLL_OPTION_INVALID(HttpStatus.BAD_REQUEST, "이 투표에 속하지 않는 선택지입니다."),
    POLL_ALREADY_VOTED(HttpStatus.CONFLICT, "이미 참여한 투표입니다."),
    ;

    private final HttpStatus status;
    private final String message;

    PollErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
