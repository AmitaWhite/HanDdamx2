package com.white.handdam.subscription.controller;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.GlobalExceptionHandler;
import com.white.handdam.subscription.dto.response.FreeSubscriptionResponse;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import com.white.handdam.subscription.service.SubscriptionService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SubscriptionControllerTest {

    private static final Long SUBSCRIBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Long SUBSCRIPTION_ID = 10L;
    private static final Instant STARTED_AT = Instant.parse("2026-07-13T00:00:00Z");

    private SubscriptionService subscriptionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        subscriptionService = Mockito.mock(SubscriptionService.class);
        mockMvc = standaloneSetup(new SubscriptionController(subscriptionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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

        mockMvc.perform(post("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .header("X-User-Id", SUBSCRIBER_ID)
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
        mockMvc.perform(delete("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .header("X-User-Id", SUBSCRIBER_ID)
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

        mockMvc.perform(post("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .header("X-User-Id", SUBSCRIBER_ID)
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

        mockMvc.perform(delete("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .header("X-User-Id", SUBSCRIBER_ID)
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
    @DisplayName("POST without X-User-Id follows current GlobalExceptionHandler fallback")
    void createFreeSubscriptionRejectsMissingUserIdHeader() throws Exception {
        mockMvc.perform(post("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verifyNoInteractions(subscriptionService);
    }

    @Test
    @DisplayName("DELETE without X-User-Id follows current GlobalExceptionHandler fallback")
    void cancelFreeSubscriptionRejectsMissingUserIdHeader() throws Exception {
        mockMvc.perform(delete("/api/creators/{creatorId}/free-subscriptions", CREATOR_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.traceId").value(not(emptyOrNullString())));

        verifyNoInteractions(subscriptionService);
    }
}
