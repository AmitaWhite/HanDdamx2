package com.white.handdam.subscription.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "subscription",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_subscription",
                        columnNames = {"subscriber_id", "creator_id"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscriber_id", nullable = false)
    private Long subscriberId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_level", nullable = false, length = 50)
    private SubscriptionLevel subscriptionLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SubscriptionStatus status;

    @Column(name = "subscription_price_snapshot")
    private Integer subscriptionPriceSnapshot;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "current_period_start_at")
    private Instant currentPeriodStartAt;

    @Column(name = "current_period_end_at")
    private Instant currentPeriodEndAt;

    @Column(name = "cancel_scheduled_at")
    private Instant cancelScheduledAt;

    private Subscription(
            Long subscriberId,
            Long creatorId,
            SubscriptionLevel subscriptionLevel,
            SubscriptionStatus status,
            Integer subscriptionPriceSnapshot,
            boolean autoRenew,
            Instant startedAt,
            Instant currentPeriodStartAt,
            Instant currentPeriodEndAt,
            Instant cancelScheduledAt
    ) {
        this.subscriberId = subscriberId;
        this.creatorId = creatorId;
        this.subscriptionLevel = subscriptionLevel;
        this.status = status;
        this.subscriptionPriceSnapshot = subscriptionPriceSnapshot;
        this.autoRenew = autoRenew;
        this.startedAt = startedAt;
        this.currentPeriodStartAt = currentPeriodStartAt;
        this.currentPeriodEndAt = currentPeriodEndAt;
        this.cancelScheduledAt = cancelScheduledAt;
    }

    public static Subscription createFree(Long subscriberId, Long creatorId, Instant startedAt) {
        validateRequired(subscriberId, "subscriberId");
        validateRequired(creatorId, "creatorId");
        validateRequired(startedAt, "startedAt");
        validateNotSelfSubscription(subscriberId, creatorId);

        return new Subscription(
                subscriberId,
                creatorId,
                SubscriptionLevel.FREE,
                SubscriptionStatus.ACTIVE,
                null,
                false,
                startedAt,
                null,
                null,
                null
        );
    }

    public boolean isFree() {
        return subscriptionLevel == SubscriptionLevel.FREE;
    }

    private static void validateNotSelfSubscription(Long subscriberId, Long creatorId) {
        if (subscriberId.equals(creatorId)) {
            throw new CustomException(SubscriptionErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED);
        }
    }

    private static void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
