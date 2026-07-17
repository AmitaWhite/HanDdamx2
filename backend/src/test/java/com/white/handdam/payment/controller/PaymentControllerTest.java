package com.white.handdam.payment.controller;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.GlobalExceptionHandler;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.member.entity.Role;
import com.white.handdam.payment.dto.request.PaymentConfirmRequest;
import com.white.handdam.payment.dto.request.PaymentFailRequest;
import com.white.handdam.payment.dto.request.PaymentPrepareRequest;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.response.PaymentDetailResponse;
import com.white.handdam.payment.dto.response.PaymentFailResponse;
import com.white.handdam.payment.dto.response.PaymentPrepareResponse;
import com.white.handdam.payment.dto.response.PaymentSummaryResponse;
import com.white.handdam.payment.entity.PaymentStatus;
import com.white.handdam.payment.exception.PaymentErrorCode;
import com.white.handdam.payment.service.PaymentService;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver()
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/payments/me returns current member payment slice")
    void getMyPayments() throws Exception {
        PaymentSummaryResponse response = new PaymentSummaryResponse(
                PAYMENT_ID,
                CREATOR_ID,
                "creator",
                "https://image/creator.png",
                15000,
                PaymentStatus.SUCCESS,
                "CARD",
                PAID_AT,
                PAID_AT
        );
        when(paymentService.getMyPayments(eq(MEMBER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(
                        List.of(response),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                        false
                ));
        authenticate();

        mockMvc.perform(get("/api/payments/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.data.content[0].creatorId").value(CREATOR_ID))
                .andExpect(jsonPath("$.data.content[0].creatorNickname").value("creator"))
                .andExpect(jsonPath("$.data.content[0].creatorProfileImageUrl").value("https://image/creator.png"))
                .andExpect(jsonPath("$.data.content[0].amount").value(15000))
                .andExpect(jsonPath("$.data.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.error").value(nullValue()));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(paymentService).getMyPayments(eq(MEMBER_ID), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} returns current member payment detail")
    void getPayment() throws Exception {
        PaymentDetailResponse response = new PaymentDetailResponse(
                PAYMENT_ID,
                SUBSCRIPTION_ID,
                CREATOR_ID,
                "creator",
                null,
                15000,
                PaymentStatus.SUCCESS,
                "CARD",
                null,
                PAID_AT,
                PAID_AT
        );
        when(paymentService.getPayment(MEMBER_ID, PAYMENT_ID)).thenReturn(response);
        authenticate();

        mockMvc.perform(get("/api/payments/{paymentId}", PAYMENT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(PAYMENT_ID))
                .andExpect(jsonPath("$.data.subscriptionId").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.data.creatorId").value(CREATOR_ID))
                .andExpect(jsonPath("$.data.creatorNickname").value("creator"))
                .andExpect(jsonPath("$.data.amount").value(15000))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.data.failureCode").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(paymentService).getPayment(MEMBER_ID, PAYMENT_ID);
    }

    @Test
    @DisplayName("GET /api/payments/{paymentId} converts owner mismatch to common error response")
    void getPaymentRejectsOwnerMismatch() throws Exception {
        when(paymentService.getPayment(MEMBER_ID, PAYMENT_ID))
                .thenThrow(new CustomException(PaymentErrorCode.PAYMENT_OWNER_MISMATCH));
        authenticate();

        mockMvc.perform(get("/api/payments/{paymentId}", PAYMENT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("PAYMENT_OWNER_MISMATCH"))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(paymentService).getPayment(MEMBER_ID, PAYMENT_ID);
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
        authenticate();

        mockMvc.perform(post("/api/payments/prepare")
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
        authenticate();

        mockMvc.perform(post("/api/payments/confirm")
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
        authenticate();

        mockMvc.perform(post("/api/payments/fail")
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
        authenticate();

        mockMvc.perform(post("/api/payments/confirm")
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
    @DisplayName("Payment lock failure is converted to conflict response")
    void paymentLockFailure() throws Exception {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        when(paymentService.confirm(MEMBER_ID, request))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));
        authenticate();

        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentKey": "payment-key",
                                  "orderId": "HANDDAM-1234567890abcdef",
                                  "amount": 15000
                                }
                                """)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("LOCK_ACQUISITION_TIMEOUT"))
                .andExpect(jsonPath("$.error.message").value("현재 동일한 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(paymentService).confirm(MEMBER_ID, request);
    }

    @Test
    @DisplayName("Invalid prepare request returns common validation error")
    void invalidPrepareRequest() throws Exception {
        authenticate();

        mockMvc.perform(post("/api/payments/prepare")
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

    private void authenticate() {
        AuthMember authMember = new AuthMember(MEMBER_ID, Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authMember, null)
        );
    }
}
