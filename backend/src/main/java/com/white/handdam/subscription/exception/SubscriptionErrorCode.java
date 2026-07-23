package com.white.handdam.subscription.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubscriptionErrorCode implements ErrorCode {

    SELF_SUBSCRIPTION_NOT_ALLOWED(
            HttpStatus.BAD_REQUEST,
            "자기 자신은 구독할 수 없습니다."
    ),

    SUBSCRIPTION_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "이미 구독 중인 크리에이터입니다."
    ),

    FREE_SUBSCRIPTION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "무료 구독 정보를 찾을 수 없습니다."
    ),

    PAID_SUBSCRIPTION_CANNOT_BE_CANCELED_AS_FREE(
            HttpStatus.CONFLICT,
            "유료 구독은 무료 구독 취소 API로 취소할 수 없습니다."
    ),

    SUBSCRIPTION_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "Subscription was not found."
    ),

    SUBSCRIPTION_OWNER_MISMATCH(
            HttpStatus.FORBIDDEN,
            "You do not have permission to access this subscription."
    ),

    FREE_SUBSCRIPTION_CANNOT_BE_CANCEL_SCHEDULED(
            HttpStatus.CONFLICT,
            "Free subscriptions cannot be scheduled for cancellation."
    ),

    SUBSCRIPTION_CANCELLATION_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "This subscription cannot be scheduled for cancellation in its current state."
    ),

    SUBSCRIPTION_CANCEL_SCHEDULE_REVOKE_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "This subscription cancellation schedule cannot be revoked in its current state."
    ),

    SUBSCRIPTION_STATE_CONFLICT(
            HttpStatus.CONFLICT,
            "The subscription state is inconsistent."
    ),

    SUBSCRIPTION_EXPIRATION_NOT_ALLOWED(
            HttpStatus.CONFLICT,
            "This subscription cannot be expired in its current state."
    ),

    SUBSCRIPTION_PERIOD_NOT_ENDED(
            HttpStatus.CONFLICT,
            "The subscription period has not ended yet."
    );

    private final HttpStatus status;
    private final String message;
}
