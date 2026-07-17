package com.white.handdam.payment.client;

public class TossConfirmFailureException extends TossPaymentException {

    public TossConfirmFailureException(String failureCode, String safeMessage) {
        super(failureCode, safeMessage);
    }
}
