package com.white.handdam.payment.client;

public abstract class TossPaymentException extends RuntimeException {

    private final String failureCode;
    private final String safeMessage;

    protected TossPaymentException(String failureCode, String safeMessage) {
        super(safeMessage);
        this.failureCode = failureCode;
        this.safeMessage = safeMessage;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getSafeMessage() {
        return safeMessage;
    }
}
