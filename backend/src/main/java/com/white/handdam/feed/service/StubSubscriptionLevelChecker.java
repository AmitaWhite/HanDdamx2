package com.white.handdam.feed.service;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// [LYJ-030] 실제 구독 DB 조회 구현체
@Component
@RequiredArgsConstructor
public class StubSubscriptionLevelChecker implements SubscriptionLevelChecker {
    private final SubscriptionRepository subscriptionRepository;
    @Override
    public String getLevel(Long memberId, Long creatorId) {
        // TODO ACTIVE / CANCEL_SCHEDULED 둘 다 유효한 구독으로 처리 추후 만료일 체크로직 확인
        return subscriptionRepository
                .findBySubscriberIdAndCreatorId(memberId, creatorId)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                        || s.getStatus() == SubscriptionStatus.CANCEL_SCHEDULED)
                .map(s -> s.getSubscriptionLevel().name()) // "FREE" or "PAID"
                .orElse(null); // 구독 없음
    }
}