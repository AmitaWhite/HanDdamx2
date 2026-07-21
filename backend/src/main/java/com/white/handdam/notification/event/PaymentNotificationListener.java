package com.white.handdam.notification.event;

import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import com.white.handdam.payment.event.PaymentFailedEvent;
import com.white.handdam.payment.event.PaymentSucceededEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 도메인 이벤트를 알림으로 변환한다.
 *
 * <p><b>시스템 알림이라 {@code senderId} 는 항상 {@code null} 이다.</b>
 * 결제자 본인을 {@code senderId} 로 넘기면 자기알림 억제 로직에 걸려
 * 알림이 예외도 로그도 없이 사라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentNotificationListener {

	/** 시스템이 보내는 알림이라 발신자가 없다 */
	private static final Long SYSTEM_SENDER = null;

	private final NotificationService notificationService;

	/** 결제 완료 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePaymentSucceeded(PaymentSucceededEvent event) {
		try {
			notificationService.create(
				event.memberId(),
				SYSTEM_SENDER,
				NotificationType.PAYMENT_SUCCESS,
				"%,d원 결제가 완료되었습니다.".formatted(event.amount()),
				event.paymentId(),
				NotificationReferenceType.PAYMENT);
		} catch (Exception e) {
			log.error("결제 성공 알림 생성 실패: paymentId={}, orderId={}",
				event.paymentId(), event.orderId(), e);
		}
	}

	/** 결제 실패 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePaymentFailed(PaymentFailedEvent event) {
		try {
			notificationService.create(
				event.memberId(),
				SYSTEM_SENDER,
				NotificationType.PAYMENT_FAILED,
				buildFailedMessage(event.failureMessage()),
				event.paymentId(),
				NotificationReferenceType.PAYMENT);
		} catch (Exception e) {
			log.error("결제 실패 알림 생성 실패: paymentId={}, orderId={}",
				event.paymentId(), event.orderId(), e);
		}
	}

	private static String buildFailedMessage(String failureMessage) {
		if (failureMessage == null || failureMessage.isBlank()) {
			return "결제에 실패했습니다.";
		}
		return "결제에 실패했습니다. (%s)".formatted(failureMessage);
	}
}
