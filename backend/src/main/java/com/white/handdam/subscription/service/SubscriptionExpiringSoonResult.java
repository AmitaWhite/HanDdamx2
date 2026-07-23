package com.white.handdam.subscription.service;

public record SubscriptionExpiringSoonResult(
        int candidateCount,
        int publishedCount,
        int skippedCount,
        int failedCount
) {
}
