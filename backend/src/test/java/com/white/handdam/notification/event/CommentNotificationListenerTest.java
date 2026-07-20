package com.white.handdam.notification.event;

import com.white.handdam.comment.event.FeedCommentCreatedEvent;
import com.white.handdam.comment.event.FeedReplyCreatedEvent;
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
class CommentNotificationListenerTest {

	private static final Long FEED_ID = 10L;
	private static final Long COMMENT_ID = 20L;
	private static final Long REPLY_ID = 21L;
	private static final Long ACTOR_ID = 2L;
	private static final Long RECIPIENT_ID = 1L;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private CommentNotificationListener commentNotificationListener;

	@Test
	@DisplayName("피드 댓글 이벤트 - 피드 작성자에게 FEED_COMMENT 알림을 만든다")
	void handleFeedCommentCreated_success() {
		commentNotificationListener.handleFeedCommentCreated(
			new FeedCommentCreatedEvent(FEED_ID, COMMENT_ID, ACTOR_ID, RECIPIENT_ID, "댓글 내용"));

		verify(notificationService).create(
			eq(RECIPIENT_ID),
			eq(ACTOR_ID),
			eq(NotificationType.FEED_COMMENT),
			eq("댓글 내용"),
			eq(FEED_ID),
			eq(NotificationReferenceType.FEED));
	}

	@Test
	@DisplayName("피드 대댓글 이벤트 - 부모 댓글 작성자에게 FEED_REPLY 알림을 만든다")
	void handleFeedReplyCreated_success() {
		commentNotificationListener.handleFeedReplyCreated(
			new FeedReplyCreatedEvent(FEED_ID, COMMENT_ID, REPLY_ID, ACTOR_ID, RECIPIENT_ID, "답글 내용"));

		verify(notificationService).create(
			eq(RECIPIENT_ID),
			eq(ACTOR_ID),
			eq(NotificationType.FEED_REPLY),
			eq("답글 내용"),
			// 대댓글도 피드 화면으로 이동한다
			eq(FEED_ID),
			eq(NotificationReferenceType.FEED));
	}

	@Test
	@DisplayName("알림 생성이 실패해도 예외가 전파되지 않는다 (원본 도메인 보호)")
	void handleFeedCommentCreated_serviceThrows_doesNotPropagate() {
		willThrow(new RuntimeException("DB down"))
			.given(notificationService).create(
				anyLong(), anyLong(), any(NotificationType.class), anyString(), anyLong(),
				any(NotificationReferenceType.class));

		assertThatCode(() -> commentNotificationListener.handleFeedCommentCreated(
			new FeedCommentCreatedEvent(FEED_ID, COMMENT_ID, ACTOR_ID, RECIPIENT_ID, "댓글 내용")))
			.doesNotThrowAnyException();
	}
}
