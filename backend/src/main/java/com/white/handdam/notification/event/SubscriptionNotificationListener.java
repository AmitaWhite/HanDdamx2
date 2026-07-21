package com.white.handdam.notification.event;

import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import com.white.handdam.subscription.event.SubscriptionExpiringSoonEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionNotificationListener {

	private static final Long SYSTEM_SENDER = null;
	private static final String EXPIRING_MESSAGE =
		"해지 예약된 유료 구독의 이용 종료일이 3일 이내로 남았습니다.";

	private final NotificationService notificationService;

	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleSubscriptionExpiringSoon(SubscriptionExpiringSoonEvent event) {
		try {
			notificationService.create(
				event.subscriberId(),
				SYSTEM_SENDER,
				NotificationType.SUBSCRIPTION_EXPIRING,
				EXPIRING_MESSAGE,
				event.subscriptionId(),
				NotificationReferenceType.SUBSCRIPTION
			);
		} catch (Exception e) {
			log.error("Subscription expiring soon notification failed. subscriptionId={}, currentPeriodEndAt={}",
				event.subscriptionId(), event.currentPeriodEndAt(), e);
		}
	}
}
