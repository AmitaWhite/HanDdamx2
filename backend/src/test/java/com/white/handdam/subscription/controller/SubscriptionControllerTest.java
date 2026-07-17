package com.white.handdam.subscription.controller;

import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.GlobalExceptionHandler;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.member.entity.Role;
import com.white.handdam.subscription.dto.response.FreeSubscriptionResponse;
import com.white.handdam.subscription.dto.response.MySubscriptionResponse;
import com.white.handdam.subscription.dto.response.SubscriptionDetailResponse;
import com.white.handdam.subscription.dto.response.SubscriptionPlanResponse;
import com.white.handdam.subscription.dto.response.SubscriptionStatusResponse;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import com.white.handdam.subscription.service.SubscriptionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SubscriptionControllerTest {

    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Long SUBSCRIPTION_ID = 10L;
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:00Z");
    private static final Instant PERIOD_END_AT = Instant.parse("2026-08-13T00:00:00Z");
    private static final Instant CANCEL_SCHEDULED_AT = Instant.parse("2026-07-20T00:00:00Z");

    private SubscriptionService subscriptionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        subscriptionService = Mockito.mock(SubscriptionService.class);
        mockMvc = standaloneSetup(new SubscriptionController(subscriptionService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST returns 201 with ApiResponse")
    void createFreeSubscription() throws Exception {
        FreeSubscriptionResponse response = new FreeSubscriptionResponse(
                SUBSCRIPTION_ID,
                CREATOR_ID,
                SubscriptionLevel.FREE,
                SubscriptionStatus.ACTIVE,
                STARTED_AT
        );
        when(subscriptionService.createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(response);
        authenticate();

        mockMvc.perform(post("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subscriptionId").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.data.creatorId").value(CREATOR_ID))
                .andExpect(jsonPath("$.data.subscriptionLevel").value("FREE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.startedAt").value("2026-07-13T00:00:00Z"))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID);
    }

    @Test
    @DisplayName("DELETE returns 200 with ApiResponse noContent")
    void cancelFreeSubscription() throws Exception {
        authenticate();

        mockMvc.perform(delete("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).cancelFreeSubscription(SUBSCRIBER_ID, CREATOR_ID);
    }

    @Test
    @DisplayName("POST duplicate subscription returns common error response")
    void createFreeSubscriptionRejectsDuplicateSubscription() throws Exception {
        when(subscriptionService.createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID))
                .thenThrow(new CustomException(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS));
        authenticate();

        mockMvc.perform(post("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("SUBSCRIPTION_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.message")
                        .value(SubscriptionErrorCode.SUBSCRIPTION_ALREADY_EXISTS.getMessage()))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(subscriptionService).createFreeSubscription(SUBSCRIBER_ID, CREATOR_ID);
    }

    @Test
    @DisplayName("DELETE missing subscription returns common error response")
    void cancelFreeSubscriptionRejectsMissingSubscription() throws Exception {
        doThrow(new CustomException(SubscriptionErrorCode.FREE_SUBSCRIPTION_NOT_FOUND))
                .when(subscriptionService)
                .cancelFreeSubscription(SUBSCRIBER_ID, CREATOR_ID);
        authenticate();

        mockMvc.perform(delete("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("FREE_SUBSCRIPTION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message")
                        .value(SubscriptionErrorCode.FREE_SUBSCRIPTION_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(subscriptionService).cancelFreeSubscription(SUBSCRIBER_ID, CREATOR_ID);
    }

    @Test
    @DisplayName("GET subscription status returns 200 with ApiResponse")
    void getSubscriptionStatus() throws Exception {
        SubscriptionStatusResponse response = new SubscriptionStatusResponse(
                CREATOR_ID,
                true,
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                STARTED_AT,
                PERIOD_END_AT
        );
        when(subscriptionService.getSubscriptionStatus(SUBSCRIBER_ID, CREATOR_ID))
                .thenReturn(response);
        authenticate();

        mockMvc.perform(get("/api/creators/{creatorId}/subscription-status", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.creatorId").value(CREATOR_ID))
                .andExpect(jsonPath("$.data.subscribed").value(true))
                .andExpect(jsonPath("$.data.subscriptionLevel").value("PAID"))
                .andExpect(jsonPath("$.data.status").value("CANCEL_SCHEDULED"))
                .andExpect(jsonPath("$.data.startedAt").value("2026-07-13T00:00:00Z"))
                .andExpect(jsonPath("$.data.currentPeriodEndAt").value("2026-08-13T00:00:00Z"))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).getSubscriptionStatus(SUBSCRIBER_ID, CREATOR_ID);
    }

    @Test
    @DisplayName("GET my subscriptions returns a list with ApiResponse")
    void getMySubscriptions() throws Exception {
        MySubscriptionResponse response = new MySubscriptionResponse(
                SUBSCRIPTION_ID,
                CREATOR_ID,
                "creator",
                "https://image/creator.png",
                SubscriptionLevel.FREE,
                SubscriptionStatus.ACTIVE,
                STARTED_AT,
                null,
                null
        );
        when(subscriptionService.getMySubscriptions(SUBSCRIBER_ID)).thenReturn(List.of(response));
        authenticate();

        mockMvc.perform(get("/api/subscriptions/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].subscriptionId").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.data[0].creatorId").value(CREATOR_ID))
                .andExpect(jsonPath("$.data[0].creatorNickname").value("creator"))
                .andExpect(jsonPath("$.data[0].creatorProfileImageUrl").value("https://image/creator.png"))
                .andExpect(jsonPath("$.data[0].subscriptionLevel").value("FREE"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).getMySubscriptions(SUBSCRIBER_ID);
    }

    @Test
    @DisplayName("GET my subscriptions returns an empty list")
    void getMySubscriptionsReturnsEmptyList() throws Exception {
        when(subscriptionService.getMySubscriptions(SUBSCRIBER_ID)).thenReturn(List.of());
        authenticate();

        mockMvc.perform(get("/api/subscriptions/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).getMySubscriptions(SUBSCRIBER_ID);
    }

    @Test
    @DisplayName("GET subscription detail returns 200 with ApiResponse")
    void getSubscription() throws Exception {
        SubscriptionDetailResponse response = paidDetailResponse(SubscriptionStatus.ACTIVE, null);
        when(subscriptionService.getSubscription(SUBSCRIBER_ID, SUBSCRIPTION_ID)).thenReturn(response);
        authenticate();

        mockMvc.perform(get("/api/subscriptions/{subscriptionId}", SUBSCRIPTION_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subscriptionId").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.data.creatorId").value(CREATOR_ID))
                .andExpect(jsonPath("$.data.creatorNickname").value("creator"))
                .andExpect(jsonPath("$.data.subscriptionLevel").value("PAID"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.subscriptionPriceSnapshot").value(15000))
                .andExpect(jsonPath("$.data.currentPeriodEndAt").value("2026-08-13T00:00:00Z"))
                .andExpect(jsonPath("$.data.cancelScheduledAt").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).getSubscription(SUBSCRIBER_ID, SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("PATCH cancel schedule returns scheduled paid subscription")
    void scheduleCancellation() throws Exception {
        SubscriptionDetailResponse response =
                paidDetailResponse(SubscriptionStatus.CANCEL_SCHEDULED, CANCEL_SCHEDULED_AT);
        when(subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID)).thenReturn(response);
        authenticate();

        mockMvc.perform(patch("/api/subscriptions/{subscriptionId}/cancel-schedule", SUBSCRIPTION_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subscriptionId").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.data.status").value("CANCEL_SCHEDULED"))
                .andExpect(jsonPath("$.data.currentPeriodEndAt").value("2026-08-13T00:00:00Z"))
                .andExpect(jsonPath("$.data.cancelScheduledAt").value("2026-07-20T00:00:00Z"))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("PATCH cancel schedule lock failure returns conflict response")
    void scheduleCancellationLockFailure() throws Exception {
        when(subscriptionService.scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));
        authenticate();

        mockMvc.perform(patch("/api/subscriptions/{subscriptionId}/cancel-schedule", SUBSCRIPTION_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("LOCK_ACQUISITION_TIMEOUT"))
                .andExpect(jsonPath("$.error.message").value("현재 동일한 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(subscriptionService).scheduleCancellation(SUBSCRIBER_ID, SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("DELETE cancel schedule returns active paid subscription")
    void revokeCancellationSchedule() throws Exception {
        SubscriptionDetailResponse response = paidDetailResponse(SubscriptionStatus.ACTIVE, null);
        when(subscriptionService.revokeCancellationSchedule(SUBSCRIBER_ID, SUBSCRIPTION_ID)).thenReturn(response);
        authenticate();

        mockMvc.perform(delete("/api/subscriptions/{subscriptionId}/cancel-schedule", SUBSCRIPTION_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subscriptionId").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.cancelScheduledAt").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).revokeCancellationSchedule(SUBSCRIBER_ID, SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("DELETE cancel schedule lock failure returns conflict response")
    void revokeCancellationScheduleLockFailure() throws Exception {
        when(subscriptionService.revokeCancellationSchedule(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));
        authenticate();

        mockMvc.perform(delete("/api/subscriptions/{subscriptionId}/cancel-schedule", SUBSCRIPTION_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("LOCK_ACQUISITION_TIMEOUT"))
                .andExpect(jsonPath("$.error.message").value("현재 동일한 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요."))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(subscriptionService).revokeCancellationSchedule(SUBSCRIBER_ID, SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("GET subscription detail owner mismatch returns common error response")
    void getSubscriptionRejectsOwnerMismatch() throws Exception {
        when(subscriptionService.getSubscription(SUBSCRIBER_ID, SUBSCRIPTION_ID))
                .thenThrow(new CustomException(SubscriptionErrorCode.SUBSCRIPTION_OWNER_MISMATCH));
        authenticate();

        mockMvc.perform(get("/api/subscriptions/{subscriptionId}", SUBSCRIPTION_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("SUBSCRIPTION_OWNER_MISMATCH"))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(subscriptionService).getSubscription(SUBSCRIBER_ID, SUBSCRIPTION_ID);
    }

    @Test
    @DisplayName("GET subscription plans returns free and paid plans")
    void getSubscriptionPlans() throws Exception {
        when(subscriptionService.getSubscriptionPlans(CREATOR_ID))
                .thenReturn(List.of(
                        SubscriptionPlanResponse.free(CREATOR_ID),
                        SubscriptionPlanResponse.paid(CREATOR_ID, 10000, true, "monthly benefits")
                ));

        mockMvc.perform(get("/api/creators/{creatorId}/subscription-plans", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].creatorId").value(CREATOR_ID))
                .andExpect(jsonPath("$.data[0].subscriptionLevel").value("FREE"))
                .andExpect(jsonPath("$.data[0].price").value(0))
                .andExpect(jsonPath("$.data[0].available").value(true))
                .andExpect(jsonPath("$.data[0].benefitsDescription").value(nullValue()))
                .andExpect(jsonPath("$.data[1].subscriptionLevel").value("PAID"))
                .andExpect(jsonPath("$.data[1].price").value(10000))
                .andExpect(jsonPath("$.data[1].available").value(true))
                .andExpect(jsonPath("$.data[1].benefitsDescription").value("monthly benefits"))
                .andExpect(jsonPath("$.error").value(nullValue()));

        verify(subscriptionService).getSubscriptionPlans(CREATOR_ID);
    }

    @Test
    @DisplayName("GET subscription plans creator error returns common error response")
    void getSubscriptionPlansRejectsMissingCreator() throws Exception {
        when(subscriptionService.getSubscriptionPlans(CREATOR_ID))
                .thenThrow(new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));

        mockMvc.perform(get("/api/creators/{creatorId}/subscription-plans", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("CREATOR_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value(CreatorErrorCode.CREATOR_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(subscriptionService).getSubscriptionPlans(CREATOR_ID);
    }

    @Test
    @DisplayName("GET subscription plans missing creator profile returns common error response")
    void getSubscriptionPlansRejectsMissingCreatorProfile() throws Exception {
        when(subscriptionService.getSubscriptionPlans(CREATOR_ID))
                .thenThrow(new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        mockMvc.perform(get("/api/creators/{creatorId}/subscription-plans", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("CREATOR_PROFILE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message")
                        .value(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verify(subscriptionService).getSubscriptionPlans(CREATOR_ID);
    }

    private void authenticate() {
        AuthMember authMember = new AuthMember(SUBSCRIBER_ID, Role.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authMember, null)
        );
    }

    private SubscriptionDetailResponse paidDetailResponse(
            SubscriptionStatus status,
            Instant cancelScheduledAt
    ) {
        return new SubscriptionDetailResponse(
                SUBSCRIPTION_ID,
                CREATOR_ID,
                "creator",
                null,
                SubscriptionLevel.PAID,
                status,
                15000,
                STARTED_AT,
                STARTED_AT,
                PERIOD_END_AT,
                cancelScheduledAt
        );
    }
}
