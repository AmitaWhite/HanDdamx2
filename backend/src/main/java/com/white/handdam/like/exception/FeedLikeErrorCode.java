package com.white.handdam.like.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FeedLikeErrorCode implements ErrorCode {
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요한 피드입니다."),
    NOT_LIKED(HttpStatus.BAD_REQUEST, "좋아요하지 않은 피드입니다."),
    ;

    private final HttpStatus status;
    private final String message;
    FeedLikeErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
