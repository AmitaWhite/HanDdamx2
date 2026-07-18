package com.white.handdam.project.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProjectErrorCode implements ErrorCode {

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    PROJECT_FORBIDDEN(HttpStatus.FORBIDDEN, "프로젝트에 접근할 권한이 없습니다."),
    PROJECT_NOT_EMPTY(HttpStatus.CONFLICT, "피드가 포함된 프로젝트는 삭제할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ProjectErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}