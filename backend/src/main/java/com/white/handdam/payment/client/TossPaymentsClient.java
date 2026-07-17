package com.white.handdam.payment.client;

import com.white.handdam.payment.dto.toss.TossConfirmRequest;
import com.white.handdam.payment.dto.toss.TossConfirmResponse;

public interface TossPaymentsClient {

    TossConfirmResponse confirm(TossConfirmRequest request, String idempotencyKey);
}
