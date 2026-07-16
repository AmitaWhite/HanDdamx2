package com.white.handdam.payment.controller;

import com.white.handdam.global.security.jwt.JwtTokenProvider;
import com.white.handdam.member.entity.Role;
import com.white.handdam.payment.dto.request.PaymentPrepareRequest;
import com.white.handdam.payment.dto.response.PaymentPrepareResponse;
import com.white.handdam.payment.service.PaymentService;
import com.white.handdam.subscription.dto.response.SubscriptionStatusResponse;
import com.white.handdam.subscription.service.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "jwt.secret=test_secret_for_payment_subscription_security_1234567890"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentSubscriptionSecurityTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    @DisplayName("Payment API uses JWT AuthMember id")
    void paymentApiUsesJwtPrincipal() throws Exception {
        PaymentPrepareRequest request = new PaymentPrepareRequest(CREATOR_ID);
        PaymentPrepareResponse response = new PaymentPrepareResponse(
                10L,
                "HANDDAM-1234567890abcdef",
                "idempotency-key",
                "customer-key",
                CREATOR_ID,
                "creator",
                15000,
                "KRW",
                "creator paid subscription"
        );
        when(paymentService.prepare(MEMBER_ID, request)).thenReturn(response);

        mockMvc.perform(post("/api/payments/prepare")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "creatorId": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.creatorId").value(CREATOR_ID));

        verify(paymentService).prepare(MEMBER_ID, request);
    }

    @Test
    @DisplayName("Subscription API uses JWT AuthMember id")
    void subscriptionApiUsesJwtPrincipal() throws Exception {
        SubscriptionStatusResponse response =
                SubscriptionStatusResponse.notSubscribed(CREATOR_ID);
        when(subscriptionService.getSubscriptionStatus(MEMBER_ID, CREATOR_ID)).thenReturn(response);

        mockMvc.perform(get("/api/creators/{creatorId}/subscription-status", CREATOR_ID)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.creatorId").value(CREATOR_ID));

        verify(subscriptionService).getSubscriptionStatus(MEMBER_ID, CREATOR_ID);
    }

    @Test
    @DisplayName("Payment API rejects unauthenticated requests")
    void paymentApiRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/payments/prepare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "creatorId": 2
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Subscription plans API is protected by SecurityConfig")
    void subscriptionPlansApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/creators/{creatorId}/subscription-plans", CREATOR_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private String bearerToken() {
        return "Bearer " + jwtTokenProvider.createAccessToken(MEMBER_ID, Role.USER);
    }
}
