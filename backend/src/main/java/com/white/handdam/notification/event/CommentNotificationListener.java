package com.white.handdam.notification.event;

import com.white.handdam.comment.event.FeedCommentCreatedEvent;
import com.white.handdam.comment.event.FeedReplyCreatedEvent;
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
 * 피드 댓글 도메인 이벤트를 알림으로 변환한다.
 *
 * <p>수신자가 서로 다르다는 점에 주의 —
 * 댓글은 <b>피드 작성자</b>에게, 대댓글은 <b>부모 댓글 작성자</b>에게 간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentNotificationListener {

	private final NotificationService notificationService;

	/** 내 피드에 댓글이 달림 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleFeedCommentCreated(FeedCommentCreatedEvent event) {
		try {
			notificationService.create(
				event.recipientId(),
				event.actorId(),
				NotificationType.FEED_COMMENT,
				event.contentPreview(),
				event.feedId(),
				NotificationReferenceType.FEED);
		} catch (Exception e) {
			log.error("피드 댓글 알림 생성 실패: commentId={}, recipientId={}",
				event.commentId(), event.recipientId(), e);
		}
	}

	/** 내 댓글에 답글이 달림 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleFeedReplyCreated(FeedReplyCreatedEvent event) {
		try {
			notificationService.create(
				event.recipientId(),
				event.actorId(),
				NotificationType.FEED_REPLY,
				event.contentPreview(),
				// 대댓글도 피드 화면으로 이동시킨다 (댓글 단건 화면이 없다)
				event.feedId(),
				NotificationReferenceType.FEED);
		} catch (Exception e) {
			log.error("피드 대댓글 알림 생성 실패: replyId={}, recipientId={}",
				event.replyId(), event.recipientId(), e);
		}
	}
}
