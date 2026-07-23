package com.white.handdam.subscription.service;

public record SubscriptionExpirationResult(
        int candidateCount,
        int convertedCount,
        int skippedCount,
        int failedCount
) {
}
