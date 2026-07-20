package com.white.handdam.notification.event;

import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import com.white.handdam.poll.event.PollVotedEvent;
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
class PollNotificationListenerTest {

	private static final Long POLL_ID = 80L;
	private static final Long FEED_ID = 10L;
	private static final Long OPTION_ID = 81L;
	private static final Long ACTOR_ID = 2L;
	private static final Long RECIPIENT_ID = 1L;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private PollNotificationListener pollNotificationListener;

	@Test
	@DisplayName("투표 참여 이벤트 - 투표를 만든 크리에이터에게 POLL_VOTE 알림을 만든다")
	void handlePollVoted_success() {
		pollNotificationListener.handlePollVoted(
			new PollVotedEvent(POLL_ID, FEED_ID, OPTION_ID, ACTOR_ID, RECIPIENT_ID));

		verify(notificationService).create(
			eq(RECIPIENT_ID),
			eq(ACTOR_ID),
			eq(NotificationType.POLL_VOTE),
			anyString(),
			eq(POLL_ID),
			eq(NotificationReferenceType.POLL));
	}

	@Test
	@DisplayName("알림 생성이 실패해도 예외가 전파되지 않는다 (투표 트랜잭션 보호)")
	void handlePollVoted_serviceThrows_doesNotPropagate() {
		willThrow(new RuntimeException("DB down"))
			.given(notificationService).create(
				anyLong(), anyLong(), any(NotificationType.class), anyString(), anyLong(),
				any(NotificationReferenceType.class));

		assertThatCode(() -> pollNotificationListener.handlePollVoted(
			new PollVotedEvent(POLL_ID, FEED_ID, OPTION_ID, ACTOR_ID, RECIPIENT_ID)))
			.doesNotThrowAnyException();
	}
}
