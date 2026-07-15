package com.white.handdam.subscription.repository;

import com.white.handdam.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);

    List<Subscription> findBySubscriberIdOrderByStartedAtDesc(Long subscriberId);

    // 크리에이터 구독자 수 조회
    long countByCreatorId(Long creatorId);
}
