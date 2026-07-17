package com.white.handdam.comment.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FeedCommentErrorCode implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_CANNOT_REPLY(HttpStatus.BAD_REQUEST, "대댓글에는 대댓글을 달 수 없습니다."),
    ;
    private final HttpStatus status;
    private final String message;
    FeedCommentErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
