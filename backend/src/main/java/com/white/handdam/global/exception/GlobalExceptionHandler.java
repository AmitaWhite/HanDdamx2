package com.white.handdam.global.exception;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.response.ErrorResponse;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 도메인에서 의도적으로 던진 예외 (CommonErrorCode든 도메인별 ErrorCode든 동일하게 처리)
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        String traceId = generateTraceId();
        log.warn("[{}] {} - {}", traceId, e.getErrorCode().name(), e.getMessage());
        return toResponse(e.getErrorCode(), e.getMessage(), traceId);
    }

    // @Valid 바디 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String traceId = generateTraceId();
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[{}] INVALID_REQUEST - {}", traceId, message);
        return toResponse(CommonErrorCode.INVALID_REQUEST, message, traceId);
    }

    // @RequestParam/@PathVariable 등 파라미터 검증 실패
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String traceId = generateTraceId();
        log.warn("[{}] INVALID_REQUEST - {}", traceId, e.getMessage());
        return toResponse(CommonErrorCode.INVALID_REQUEST, e.getMessage(), traceId);
    }

    // 그 외 모든 예외 (예상 못한 서버 오류)
    // Lock acquisition timeout
    @ExceptionHandler({
            PessimisticLockingFailureException.class,
            LockTimeoutException.class,
            PessimisticLockException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleLockAcquisitionFailure(Exception e) {
        String traceId = generateTraceId();
        log.warn("[{}] LOCK_ACQUISITION_TIMEOUT - {}", traceId, e.getMessage());
        return toResponse(LockErrorCode.LOCK_ACQUISITION_TIMEOUT, traceId);
    }

    // Other unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        String traceId = generateTraceId();
        log.error("[{}] INTERNAL_ERROR", traceId, e);
        return toResponse(CommonErrorCode.INTERNAL_ERROR, traceId);
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, String traceId) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(ErrorResponse.of(errorCode, traceId)));
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(ErrorCode errorCode, String message, String traceId) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(ErrorResponse.of(errorCode, message, traceId)));
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
