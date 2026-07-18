package com.white.handdam.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LockErrorCode implements ErrorCode {

    LOCK_ACQUISITION_TIMEOUT(
            HttpStatus.CONFLICT,
            "현재 동일한 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요."
    );

    private final HttpStatus status;
    private final String message;
}
