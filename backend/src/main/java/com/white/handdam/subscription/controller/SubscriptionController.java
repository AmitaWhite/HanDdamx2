package com.white.handdam.subscription.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.subscription.dto.response.FreeSubscriptionResponse;
import com.white.handdam.subscription.dto.response.MySubscriptionResponse;
import com.white.handdam.subscription.dto.response.SubscriptionDetailResponse;
import com.white.handdam.subscription.dto.response.SubscriptionPlanResponse;
import com.white.handdam.subscription.dto.response.SubscriptionStatusResponse;
import com.white.handdam.subscription.service.SubscriptionService;
import com.white.handdam.global.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/creators/{creatorId}/free-subscriptions")
    public ApiResponse<FreeSubscriptionResponse> createFreeSubscription(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long creatorId
    ) {
        FreeSubscriptionResponse response = subscriptionService.createFreeSubscription(authMember.id(), creatorId);

        return ApiResponse.success(response);
    }

    @DeleteMapping("/api/creators/{creatorId}/free-subscriptions")
    public ApiResponse<Void> cancelFreeSubscription(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long creatorId
    ) {
        subscriptionService.cancelFreeSubscription(authMember.id(), creatorId);

        return ApiResponse.noContent();
    }

    @GetMapping("/api/creators/{creatorId}/subscription-status")
    public ApiResponse<SubscriptionStatusResponse> getSubscriptionStatus(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long creatorId
    ) {
        SubscriptionStatusResponse response =
                subscriptionService.getSubscriptionStatus(authMember.id(), creatorId);

        return ApiResponse.success(response);
    }

    @GetMapping("/api/subscriptions/me")
    public ApiResponse<List<MySubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        List<MySubscriptionResponse> response = subscriptionService.getMySubscriptions(authMember.id());

        return ApiResponse.success(response);
    }

    @GetMapping("/api/subscriptions/{subscriptionId}")
    public ApiResponse<SubscriptionDetailResponse> getSubscription(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long subscriptionId
    ) {
        SubscriptionDetailResponse response =
                subscriptionService.getSubscription(authMember.id(), subscriptionId);

        return ApiResponse.success(response);
    }

    @PatchMapping("/api/subscriptions/{subscriptionId}/cancel-schedule")
    public ApiResponse<SubscriptionDetailResponse> scheduleCancellation(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long subscriptionId
    ) {
        SubscriptionDetailResponse response =
                subscriptionService.scheduleCancellation(authMember.id(), subscriptionId);

        return ApiResponse.success(response);
    }

    @DeleteMapping("/api/subscriptions/{subscriptionId}/cancel-schedule")
    public ApiResponse<SubscriptionDetailResponse> revokeCancellationSchedule(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long subscriptionId
    ) {
        SubscriptionDetailResponse response =
                subscriptionService.revokeCancellationSchedule(authMember.id(), subscriptionId);

        return ApiResponse.success(response);
    }

    @GetMapping("/api/creators/{creatorId}/subscription-plans")
    public ApiResponse<List<SubscriptionPlanResponse>> getSubscriptionPlans(
            @PathVariable Long creatorId
    ) {
        List<SubscriptionPlanResponse> response = subscriptionService.getSubscriptionPlans(creatorId);

        return ApiResponse.success(response);
    }
}
