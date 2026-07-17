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
import java.time.ZoneOffset;

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

    public static Subscription createPaid(
            Long subscriberId,
            Long creatorId,
            Integer subscriptionPriceSnapshot,
            Instant approvedAt
    ) {
        validateRequired(subscriberId, "subscriberId");
        validateRequired(creatorId, "creatorId");
        validateRequired(subscriptionPriceSnapshot, "subscriptionPriceSnapshot");
        validateRequired(approvedAt, "approvedAt");
        validatePositive(subscriptionPriceSnapshot, "subscriptionPriceSnapshot");
        validateNotSelfSubscription(subscriberId, creatorId);

        return new Subscription(
                subscriberId,
                creatorId,
                SubscriptionLevel.PAID,
                SubscriptionStatus.ACTIVE,
                subscriptionPriceSnapshot,
                false,
                approvedAt,
                approvedAt,
                plusOneMonth(approvedAt),
                null
        );
    }

    public void upgradeToPaid(Integer subscriptionPriceSnapshot, Instant approvedAt) {
        validateRequired(subscriptionPriceSnapshot, "subscriptionPriceSnapshot");
        validateRequired(approvedAt, "approvedAt");
        validatePositive(subscriptionPriceSnapshot, "subscriptionPriceSnapshot");

        if (subscriptionLevel == SubscriptionLevel.PAID) {
            throw new IllegalStateException("Already paid subscription.");
        }

        subscriptionLevel = SubscriptionLevel.PAID;
        status = SubscriptionStatus.ACTIVE;
        this.subscriptionPriceSnapshot = subscriptionPriceSnapshot;
        autoRenew = false;
        currentPeriodStartAt = approvedAt;
        currentPeriodEndAt = plusOneMonth(approvedAt);
        cancelScheduledAt = null;
    }

    public void scheduleCancellation(Instant cancelRequestedAt) {
        validateRequired(cancelRequestedAt, "cancelRequestedAt");

        if (isFree()) {
            throw new CustomException(SubscriptionErrorCode.FREE_SUBSCRIPTION_CANNOT_BE_CANCEL_SCHEDULED);
        }

        if (status == SubscriptionStatus.CANCEL_SCHEDULED) {
            return;
        }

        if (status != SubscriptionStatus.ACTIVE) {
            throw new CustomException(SubscriptionErrorCode.SUBSCRIPTION_CANCELLATION_NOT_ALLOWED);
        }

        if (currentPeriodEndAt == null) {
            throw new CustomException(SubscriptionErrorCode.SUBSCRIPTION_STATE_CONFLICT);
        }

        status = SubscriptionStatus.CANCEL_SCHEDULED;
        cancelScheduledAt = cancelRequestedAt;
    }

    public void revokeCancellationSchedule() {
        if (isFree()) {
            throw new CustomException(SubscriptionErrorCode.FREE_SUBSCRIPTION_CANNOT_BE_CANCEL_SCHEDULED);
        }

        if (status == SubscriptionStatus.ACTIVE && cancelScheduledAt == null) {
            return;
        }

        if (status != SubscriptionStatus.CANCEL_SCHEDULED) {
            throw new CustomException(SubscriptionErrorCode.SUBSCRIPTION_CANCEL_SCHEDULE_REVOKE_NOT_ALLOWED);
        }

        status = SubscriptionStatus.ACTIVE;
        cancelScheduledAt = null;
    }

    public boolean isFree() {
        return subscriptionLevel == SubscriptionLevel.FREE;
    }

    public boolean isPaid() {
        return subscriptionLevel == SubscriptionLevel.PAID;
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

    private static void validatePositive(Integer value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private static Instant plusOneMonth(Instant approvedAt) {
        return approvedAt.atZone(ZoneOffset.UTC)
                .plusMonths(1)
                .toInstant();
    }
}
