package com.white.handdam.payment.client;

public class TossTimeoutException extends TossPaymentException {

    public TossTimeoutException(String failureCode, String safeMessage) {
        super(failureCode, safeMessage);
    }
}
