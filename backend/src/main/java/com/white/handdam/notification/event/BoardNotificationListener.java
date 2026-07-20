package com.white.handdam.notification.event;

import com.white.handdam.board.event.BoardAnswerCreatedEvent;
import com.white.handdam.board.event.BoardCommentCreatedEvent;
import com.white.handdam.board.event.BoardReplyCreatedEvent;
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
 * 유료 게시판 도메인 이벤트를 알림으로 변환한다.
 *
 * <p>세 종류 모두 알림 클릭 시 게시글 화면으로 이동한다({@code BOARD_POST}).
 * 수신자는 각각 다르다 — 댓글·답변은 <b>게시글 작성자</b>, 대댓글은 <b>부모 댓글 작성자</b>.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BoardNotificationListener {

	private final NotificationService notificationService;

	/** 내 게시글에 댓글이 달림 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleBoardCommentCreated(BoardCommentCreatedEvent event) {
		try {
			notificationService.create(
				event.recipientId(),
				event.actorId(),
				NotificationType.BOARD_COMMENT,
				event.contentPreview(),
				event.postId(),
				NotificationReferenceType.BOARD_POST);
		} catch (Exception e) {
			log.error("게시판 댓글 알림 생성 실패: commentId={}, recipientId={}",
				event.commentId(), event.recipientId(), e);
		}
	}

	/** 내 댓글에 답글이 달림 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleBoardReplyCreated(BoardReplyCreatedEvent event) {
		try {
			notificationService.create(
				event.recipientId(),
				event.actorId(),
				NotificationType.BOARD_REPLY,
				event.contentPreview(),
				event.postId(),
				NotificationReferenceType.BOARD_POST);
		} catch (Exception e) {
			log.error("게시판 대댓글 알림 생성 실패: replyId={}, recipientId={}",
				event.replyId(), event.recipientId(), e);
		}
	}

	/** 내 질문에 크리에이터가 공식 답변을 남김 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleBoardAnswerCreated(BoardAnswerCreatedEvent event) {
		try {
			notificationService.create(
				event.recipientId(),
				event.actorId(),
				NotificationType.BOARD_ANSWER,
				event.contentPreview(),
				event.postId(),
				NotificationReferenceType.BOARD_POST);
		} catch (Exception e) {
			log.error("게시판 답변 알림 생성 실패: answerId={}, recipientId={}",
				event.answerId(), event.recipientId(), e);
		}
	}
}
