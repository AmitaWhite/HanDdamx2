package com.white.handdam.notification.entity;

import com.white.handdam.global.entity.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "notification")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationEntity extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    public enum NotificationType {
        // feed
        NEW_FEED,
        FEED_COMMENT,
        FEED_REPLY,
        // board
        BOARD_ANSWER,
        BOARD_COMMENT,
        BOARD_REPLY,
        // chat
        CHAT_MESSAGE,
        // subscription
        SUBSCRIPTION_EXPIRING,
        // payment
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        // poll
        POLL_VOTE,
        // related with creator activity
        CREATOR_APPLICATION_APPROVED,
        CREATOR_APPLICATION_REJECTED
    }

    @Column(length = 500)
    private String message;

    @Column(nullable = false)
    private Long referenceId;

    @Column(nullable = false, length = 50)
    private String referenceType;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Builder
    private NotificationEntity(
            Long memberId,
            Long senderId,
            NotificationType type,
            String message,
            Long referenceId,
            String referenceType) {
        this.memberId = memberId;
        this.senderId = senderId;
        this.type = type;
        this.message = message;
        this.referenceId = referenceId;
        this.referenceType = referenceType;
    }

    /* entity method */
    public void markAsRead() {
        this.isRead = true;
    }

}
