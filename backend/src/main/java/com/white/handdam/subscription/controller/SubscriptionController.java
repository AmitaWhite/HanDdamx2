package com.white.handdam.subscription.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.subscription.dto.response.FreeSubscriptionResponse;
import com.white.handdam.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/creators")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{creatorId}/free-subscriptions")
    public ResponseEntity<ApiResponse<FreeSubscriptionResponse>> createFreeSubscription(
            // TODO: X-User-Id is temporary and is not real authentication.
            // Remove it after the auth module is complete, then read memberId from SecurityContext or authenticated Principal.
            @RequestHeader("X-User-Id") Long subscriberId,
            @PathVariable Long creatorId
    ) {
        FreeSubscriptionResponse response = subscriptionService.createFreeSubscription(subscriberId, creatorId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @DeleteMapping("/{creatorId}/free-subscriptions")
    public ResponseEntity<ApiResponse<Void>> cancelFreeSubscription(
            // TODO: X-User-Id is temporary and is not real authentication.
            // Remove it after the auth module is complete, then read memberId from SecurityContext or authenticated Principal.
            @RequestHeader("X-User-Id") Long subscriberId,
            @PathVariable Long creatorId
    ) {
        subscriptionService.cancelFreeSubscription(subscriberId, creatorId);

        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
