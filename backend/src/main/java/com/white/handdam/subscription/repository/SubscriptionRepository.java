package com.white.handdam.subscription.repository;

import com.white.handdam.subscription.entity.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select subscription
            from Subscription subscription
            where subscription.id = :subscriptionId
            """)
    Optional<Subscription> findByIdForUpdate(@Param("subscriptionId") Long subscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select subscription
            from Subscription subscription
            where subscription.subscriberId = :subscriberId
              and subscription.creatorId = :creatorId
            """)
    Optional<Subscription> findBySubscriberIdAndCreatorIdForUpdate(
            @Param("subscriberId") Long subscriberId,
            @Param("creatorId") Long creatorId
    );

    List<Subscription> findBySubscriberIdOrderByStartedAtDesc(Long subscriberId);

    // 크리에이터 구독자 수 조회
    long countByCreatorId(Long creatorId);
}
