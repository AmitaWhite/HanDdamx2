package com.white.handdam.payment.controller;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.GlobalExceptionHandler;
import com.white.handdam.payment.dto.request.PaymentConfirmRequest;
import com.white.handdam.payment.dto.request.PaymentFailRequest;
import com.white.handdam.payment.dto.request.PaymentPrepareRequest;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.response.PaymentFailResponse;
import com.white.handdam.payment.dto.response.PaymentPrepareResponse;
import com.white.handdam.payment.entity.PaymentStatus;
import com.white.handdam.payment.exception.PaymentErrorCode;
import com.white.handdam.payment.service.PaymentService;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PaymentControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Long PAYMENT_ID = 10L;
    private static final Long SUBSCRIPTION_ID = 20L;
    private static final String ORDER_ID = "HANDDAM-1234567890abcdef";
    private static final String PAYMENT_KEY = "payment-key";
    private static final Instant PAID_AT = Instant.parse("2026-07-15T01:00:00Z");
    private static final Instant PERIOD_END_AT = Instant.parse("2026-08-15T01:00:00Z");

    private PaymentService paymentService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        paymentService = Mockito.mock(PaymentService.class);
        mockMvc = standaloneSetup(new PaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/payments/prepare returns 201 with ApiResponse")
    void prepare() throws Exception {
        PaymentPrepareResponse response = new PaymentPrepareResponse(
                PAYMENT_ID,
                ORDER_ID,
                "idempotency-key",
                "customer-key",
                CREATOR_ID,
                "creator",
                15000,
                "KRW",
                "creator 작가 월간 유료 구독"
        );
        PaymentPrepareRequest request = new PaymentPrepareRequest(CREATOR_ID);
        when(paymentService.prepare(MEMBER_ID, request)).thenReturn(response);

        mockMvc.perform(post("/api/payments/prepare")
                        .header("X-User-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "creatorId": 2
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.data.idempotencyKey").value("idempotency-key"))
                .andExpect(jsonPath("$.data.customerKey").value("customer-key"))
                .andExpect(jsonPath("$.data.creatorId").value(CREATOR_ID))
                .andExpect(jsonPath("$.data.creatorNickname").value("creator"))
                .andExpect(jsonPath("$.data.amount").value(15000))
                .andExpect(jsonPath("$.data.currency").value("KRW"))
                .andExpect(jsonPath("$.data.orderName").value("creator 작가 월간 유료 구독"))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(paymentService).prepare(MEMBER_ID, request);
    }

    @Test
    @DisplayName("POST /api/payments/confirm returns 200 with internal response DTO")
    void confirm() throws Exception {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        PaymentConfirmResponse response = new PaymentConfirmResponse(
                PAYMENT_ID,
                ORDER_ID,
                PAYMENT_KEY,
                15000,
                PaymentStatus.SUCCESS,
                "카드",
                PAID_AT,
                SUBSCRIPTION_ID,
                SubscriptionLevel.PAID,
                PAID_AT,
                PERIOD_END_AT
        );
        when(paymentService.confirm(MEMBER_ID, request)).thenReturn(response);

        mockMvc.perform(post("/api/payments/confirm")
                        .header("X-User-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentKey": "payment-key",
                                  "orderId": "HANDDAM-1234567890abcdef",
                                  "amount": 15000
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.data.paymentKey").value(PAYMENT_KEY))
                .andExpect(jsonPath("$.data.amount").value(15000))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paymentMethod").value("카드"))
                .andExpect(jsonPath("$.data.subscriptionId").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.data.subscriptionLevel").value("PAID"))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(paymentService).confirm(MEMBER_ID, request);
    }

    @Test
    @DisplayName("POST /api/payments/fail returns 200 and does not expose failure message")
    void fail() throws Exception {
        PaymentFailRequest request = new PaymentFailRequest(ORDER_ID, "PAY_PROCESS_CANCELED", "canceled");
        PaymentFailResponse response = new PaymentFailResponse(
                PAYMENT_ID,
                ORDER_ID,
                PaymentStatus.FAILED,
                "PAY_PROCESS_CANCELED"
        );
        when(paymentService.fail(MEMBER_ID, request)).thenReturn(response);

        mockMvc.perform(post("/api/payments/fail")
                        .header("X-User-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "HANDDAM-1234567890abcdef",
                                  "code": "PAY_PROCESS_CANCELED",
                                  "message": "canceled"
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.data.orderId").value(ORDER_ID))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureCode").value("PAY_PROCESS_CANCELED"))
                .andExpect(jsonPath("$.data.failureMessage").doesNotExist())
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(paymentService).fail(MEMBER_ID, request);
    }

    @Test
    @DisplayName("Payment CustomException is converted to common error response")
    void paymentError() throws Exception {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        when(paymentService.confirm(MEMBER_ID, request))
                .thenThrow(new CustomException(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH));

        mockMvc.perform(post("/api/payments/confirm")
                        .header("X-User-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentKey": "payment-key",
                                  "orderId": "HANDDAM-1234567890abcdef",
                                  "amount": 15000
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("PAYMENT_AMOUNT_MISMATCH"))
                .andExpect(jsonPath("$.error.message").value(PaymentErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage()))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(paymentService).confirm(MEMBER_ID, request);
    }

    @Test
    @DisplayName("Invalid prepare request returns common validation error")
    void invalidPrepareRequest() throws Exception {
        mockMvc.perform(post("/api/payments/prepare")
                        .header("X-User-Id", MEMBER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "creatorId": null
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));
    }
}
