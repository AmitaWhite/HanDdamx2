package com.white.handdam.subscription.repository;

import com.white.handdam.global.persistence.JpaLockHints;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberIdAndCreatorId(Long subscriberId, Long creatorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(
            name = JpaLockHints.LOCK_TIMEOUT_HINT,
            value = JpaLockHints.LOCK_TIMEOUT_MILLIS
    ))
    @Query("""
            select subscription
            from Subscription subscription
            where subscription.id = :subscriptionId
            """)
    Optional<Subscription> findByIdForUpdate(@Param("subscriptionId") Long subscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(
            name = JpaLockHints.LOCK_TIMEOUT_HINT,
            value = JpaLockHints.LOCK_TIMEOUT_MILLIS
    ))
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

    @Query("""
            select subscription.id
            from Subscription subscription
            where subscription.subscriptionLevel = :subscriptionLevel
              and subscription.status = :status
              and subscription.currentPeriodEndAt is not null
              and subscription.currentPeriodEndAt <= :now
            order by subscription.currentPeriodEndAt asc, subscription.id asc
            """)
    List<Long> findExpiredSubscriptionIds(
            @Param("subscriptionLevel") SubscriptionLevel subscriptionLevel,
            @Param("status") SubscriptionStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    List<Subscription> findBySubscriberIdOrderByStartedAtDesc(Long subscriberId);

    // 크리에이터 구독자 수 조회
    long countByCreatorId(Long creatorId);

    // KSY-015
    long countBySubscriberId(Long subscriberId);
}
