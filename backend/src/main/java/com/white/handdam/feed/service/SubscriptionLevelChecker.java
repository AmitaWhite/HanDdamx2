package com.white.handdam.feed.service;

import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// TODO ACTIVE / CANCEL_SCHEDULED 둘 다 유효한 구독으로 처리 — 추후 만료일 체크 로직 확인
@Component
@RequiredArgsConstructor
public class SubscriptionLevelChecker {
    private final SubscriptionRepository subscriptionRepository;

    // 반환값: "FREE" | "PAID" | null(비구독)
    public String getLevel(Long memberId, Long creatorId) {
        return subscriptionRepository
                .findBySubscriberIdAndCreatorId(memberId, creatorId)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                        || s.getStatus() == SubscriptionStatus.CANCEL_SCHEDULED)
                .map(s -> s.getSubscriptionLevel().name())
                .orElse(null);
    }
}
