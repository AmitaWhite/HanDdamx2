package com.white.handdam.notification.event;

import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import com.white.handdam.poll.event.PollVotedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 투표 도메인 이벤트를 알림으로 변환한다.
 *
 * <p>투표를 만든 크리에이터에게 참여 사실을 알린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PollNotificationListener {

	private final NotificationService notificationService;

	/** 내 투표에 누군가 참여함 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePollVoted(PollVotedEvent event) {
		try {
			notificationService.create(
				event.recipientId(),
				event.actorId(),
				NotificationType.POLL_VOTE,
				"투표에 새로운 참여가 있습니다.",
				event.pollId(),
				NotificationReferenceType.POLL);
		} catch (Exception e) {
			log.error("투표 알림 생성 실패: pollId={}, recipientId={}",
				event.pollId(), event.recipientId(), e);
		}
	}
}
