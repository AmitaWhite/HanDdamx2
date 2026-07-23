package com.white.handdam.notification.event;

import com.white.handdam.board.event.BoardAnswerCreatedEvent;
import com.white.handdam.board.event.BoardCommentCreatedEvent;
import com.white.handdam.board.event.BoardReplyCreatedEvent;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BoardNotificationListenerTest {

	private static final Long POST_ID = 30L;
	private static final Long COMMENT_ID = 40L;
	private static final Long REPLY_ID = 41L;
	private static final Long ANSWER_ID = 50L;
	private static final Long ACTOR_ID = 2L;
	private static final Long RECIPIENT_ID = 1L;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private BoardNotificationListener boardNotificationListener;

	@Test
	@DisplayName("게시판 댓글 이벤트 - 게시글 작성자에게 BOARD_COMMENT 알림을 만든다")
	void handleBoardCommentCreated_success() {
		boardNotificationListener.handleBoardCommentCreated(
			new BoardCommentCreatedEvent(POST_ID, COMMENT_ID, ACTOR_ID, RECIPIENT_ID, "댓글 내용"));

		verify(notificationService).create(
			eq(RECIPIENT_ID),
			eq(ACTOR_ID),
			eq(NotificationType.BOARD_COMMENT),
			eq("댓글 내용"),
			eq(POST_ID),
			eq(NotificationReferenceType.BOARD_POST));
	}

	@Test
	@DisplayName("게시판 대댓글 이벤트 - 부모 댓글 작성자에게 BOARD_REPLY 알림을 만든다")
	void handleBoardReplyCreated_success() {
		boardNotificationListener.handleBoardReplyCreated(
			new BoardReplyCreatedEvent(POST_ID, COMMENT_ID, REPLY_ID, ACTOR_ID, RECIPIENT_ID, "답글 내용"));

		verify(notificationService).create(
			eq(RECIPIENT_ID),
			eq(ACTOR_ID),
			eq(NotificationType.BOARD_REPLY),
			eq("답글 내용"),
			eq(POST_ID),
			eq(NotificationReferenceType.BOARD_POST));
	}

	@Test
	@DisplayName("공식 답변 이벤트 - 질문자에게 BOARD_ANSWER 알림을 만든다")
	void handleBoardAnswerCreated_success() {
		boardNotificationListener.handleBoardAnswerCreated(
			new BoardAnswerCreatedEvent(POST_ID, ANSWER_ID, ACTOR_ID, RECIPIENT_ID, "답변 내용"));

		verify(notificationService).create(
			eq(RECIPIENT_ID),
			eq(ACTOR_ID),
			eq(NotificationType.BOARD_ANSWER),
			eq("답변 내용"),
			eq(POST_ID),
			eq(NotificationReferenceType.BOARD_POST));
	}

	@Test
	@DisplayName("알림 생성이 실패해도 예외가 전파되지 않는다 (원본 도메인 보호)")
	void handleBoardAnswerCreated_serviceThrows_doesNotPropagate() {
		willThrow(new RuntimeException("DB down"))
			.given(notificationService).create(
				anyLong(), anyLong(), any(NotificationType.class), anyString(), anyLong(),
				any(NotificationReferenceType.class));

		assertThatCode(() -> boardNotificationListener.handleBoardAnswerCreated(
			new BoardAnswerCreatedEvent(POST_ID, ANSWER_ID, ACTOR_ID, RECIPIENT_ID, "답변 내용")))
			.doesNotThrowAnyException();
	}
}
