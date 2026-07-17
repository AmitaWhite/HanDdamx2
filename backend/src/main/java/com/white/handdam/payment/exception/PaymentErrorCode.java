package com.white.handdam.payment.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "결제 정보를 찾을 수 없습니다."
    ),

    PAYMENT_OWNER_MISMATCH(
            HttpStatus.FORBIDDEN,
            "해당 결제에 접근할 권한이 없습니다."
    ),

    PAYMENT_AMOUNT_MISMATCH(
            HttpStatus.BAD_REQUEST,
            "결제 금액이 일치하지 않습니다."
    ),

    PAYMENT_NOT_CONFIRMABLE(
            HttpStatus.CONFLICT,
            "현재 상태에서는 결제를 승인할 수 없습니다."
    ),

    PAYMENT_ALREADY_FAILED(
            HttpStatus.CONFLICT,
            "이미 실패 처리된 결제입니다."
    ),

    PAYMENT_CONFLICT(
            HttpStatus.CONFLICT,
            "결제 상태가 변경되어 요청을 처리할 수 없습니다."
    ),

    PAID_SUBSCRIPTION_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "이미 이용 중인 유료 구독이 있습니다."
    ),

    PAID_PLAN_NOT_AVAILABLE(
            HttpStatus.BAD_REQUEST,
            "현재 이용 가능한 유료 구독 플랜이 없습니다."
    ),

    DUPLICATE_PAYMENT_KEY(
            HttpStatus.CONFLICT,
            "이미 사용된 결제 키입니다."
    ),

    TOSS_CONFIRM_FAILED(
            HttpStatus.BAD_REQUEST,
            "Toss Payments 결제 승인이 거절되었습니다."
    ),

    TOSS_API_ERROR(
            HttpStatus.BAD_GATEWAY,
            "Toss Payments 결제 승인 요청을 처리할 수 없습니다."
    ),

    TOSS_TIMEOUT(
            HttpStatus.GATEWAY_TIMEOUT,
            "Toss Payments 결제 승인 요청이 지연되고 있습니다."
    ),

    INVALID_TOSS_RESPONSE(
            HttpStatus.BAD_GATEWAY,
            "Toss Payments 승인 응답이 올바르지 않습니다."
    );

    private final HttpStatus status;
    private final String message;
}
