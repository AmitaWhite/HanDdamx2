package com.white.handdam.feed.service;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubscriptionLevelChecker {
    private final SubscriptionRepository subscriptionRepository;

    // 반환값: "FREE" | "PAID" | null(비구독)
    public String getLevel(Long memberId, Long creatorId) {
        return subscriptionRepository
            .findBySubscriberIdAndCreatorId(memberId, creatorId)
            .filter(s -> isActiveSubscription(s))  // ← 기존 인라인 조건을 헬퍼로 위임
            .map(s -> s.getSubscriptionLevel().name())
            .orElse(null);
    }

    // [LYJ-007] 회원의 활성 구독 전체 조회 -> creatorId:level 맵 반환
    public Map<Long, SubscriptionLevel> getActiveSubscriptionLevels(Long memberId) {
        List<Subscription> subscriptions =
            subscriptionRepository.findBySubscriberIdOrderByStartedAtDesc(memberId);
        Map<Long, SubscriptionLevel> result = new HashMap<>();
        for (Subscription s : subscriptions) {
            if (isActiveSubscription(s)) {  // ← 기존 인라인 조건을 헬퍼로 위임
                result.put(s.getCreatorId(), s.getSubscriptionLevel());
            }
        }
        return result;
    }

    // 실제 유효한 구독인지 판단
    // - ACTIVE: 항상 유효
    // - CANCEL_SCHEDULED: currentPeriodEndAt이 아직 지나지 않은 경우만 유효
    private boolean isActiveSubscription(Subscription s) {
        if (s.getStatus() == SubscriptionStatus.ACTIVE) {
            return true;
        }
        if (s.getStatus() == SubscriptionStatus.CANCEL_SCHEDULED) {
            return !s.isExpiredCancelScheduledPaid(Instant.now());
        }
        return false;
    }
}
