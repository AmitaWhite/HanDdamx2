package com.white.handdam.global.response;

import com.white.handdam.global.exception.ErrorCode;

public record ErrorResponse(
        String code,
        String message,
        String traceId) {
    public static ErrorResponse of(ErrorCode errorCode, String traceId) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), traceId);
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String traceId) {
        return new ErrorResponse(errorCode.name(), message, traceId);
    }
}
