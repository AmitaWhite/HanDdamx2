package com.white.handdam.payment.client;

public class TossApiException extends TossPaymentException {

    public TossApiException(String failureCode, String safeMessage) {
        super(failureCode, safeMessage);
    }
}
