package com.white.handdam.notification.event;

import com.white.handdam.creator.event.CreatorApplicationApprovedEvent;
import com.white.handdam.creator.event.CreatorApplicationRejectedEvent;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 크리에이터 전환 심사 이벤트를 알림으로 변환한다.
 *
 * <p><b>시스템 알림이라 {@code senderId} 는 항상 {@code null} 이다.</b>
 * 심사한 관리자 ID 를 신청자에게 노출하지 않기 위해서이기도 하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreatorNotificationListener {

	/** 시스템이 보내는 알림이라 발신자가 없다 (관리자 ID 를 노출하지 않는다) */
	private static final Long SYSTEM_SENDER = null;

	private final NotificationService notificationService;

	/** 크리에이터 전환 승인 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleApplicationApproved(CreatorApplicationApprovedEvent event) {
		try {
			notificationService.create(
				event.applicantId(),
				SYSTEM_SENDER,
				NotificationType.CREATOR_APPLICATION_APPROVED,
				"크리에이터 전환 신청이 승인되었습니다.",
				event.applicationId(),
				NotificationReferenceType.CREATOR_APPLICATION);
		} catch (Exception e) {
			log.error("크리에이터 승인 알림 생성 실패: applicationId={}, applicantId={}",
				event.applicationId(), event.applicantId(), e);
		}
	}

	/** 크리에이터 전환 거절 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleApplicationRejected(CreatorApplicationRejectedEvent event) {
		try {
			notificationService.create(
				event.applicantId(),
				SYSTEM_SENDER,
				NotificationType.CREATOR_APPLICATION_REJECTED,
				buildRejectedMessage(event.rejectReason()),
				event.applicationId(),
				NotificationReferenceType.CREATOR_APPLICATION);
		} catch (Exception e) {
			log.error("크리에이터 거절 알림 생성 실패: applicationId={}, applicantId={}",
				event.applicationId(), event.applicantId(), e);
		}
	}

	private static String buildRejectedMessage(String rejectReason) {
		if (rejectReason == null || rejectReason.isBlank()) {
			return "크리에이터 전환 신청이 거절되었습니다.";
		}
		return "크리에이터 전환 신청이 거절되었습니다. (%s)".formatted(rejectReason);
	}
}
