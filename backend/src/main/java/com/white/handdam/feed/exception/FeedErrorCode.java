package com.white.handdam.feed.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FeedErrorCode implements ErrorCode {
    // LYJ-001 피드 CRUD
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    FEED_FORBIDDEN(HttpStatus.FORBIDDEN, "피드에 대한 권한이 없습니다."),
    // [LYJ-002, LYJ-030] 공개범위·잠금
    FREE_SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "무료 구독자만 접근할 수 있는 콘텐츠입니다."),
    PAID_SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "유료 구독자만 접근할 수 있는 콘텐츠입니다."),
    // [LYJ-012, LYJ-013]
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다."),
    ATTACHMENT_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "파일 또는 비디오 URL 중 하나를 제공해야 합니다."),
    ;
    private final HttpStatus status;
    private final String message;
    FeedErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
