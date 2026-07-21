package com.white.handdam.notification.event;

import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import com.white.handdam.subscription.event.SubscriptionExpiringSoonEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionNotificationListenerTest {

	private static final Long SUBSCRIPTION_ID = 10L;
	private static final Long SUBSCRIBER_ID = 1L;
	private static final Long CREATOR_ID = 2L;
	private static final Instant PERIOD_END_AT = Instant.parse("2026-08-13T00:00:00Z");

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private SubscriptionNotificationListener listener;

	@Test
	@DisplayName("subscription expiring soon event creates idempotent subscription notification")
	void handleSubscriptionExpiringSoonCreatesNotification() {
		SubscriptionExpiringSoonEvent event = event();

		listener.handleSubscriptionExpiringSoon(event);

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> dedupKeyCaptor = ArgumentCaptor.forClass(String.class);
		verify(notificationService).createIfAbsent(
			eq(SUBSCRIBER_ID),
			isNull(),
			eq(NotificationType.SUBSCRIPTION_EXPIRING),
			messageCaptor.capture(),
			eq(SUBSCRIPTION_ID),
			eq(NotificationReferenceType.SUBSCRIPTION),
			dedupKeyCaptor.capture()
		);
		assertThat(messageCaptor.getValue()).contains("3일 이내");
		assertThat(dedupKeyCaptor.getValue())
			.isEqualTo("SUBSCRIPTION_EXPIRING:10:2026-08-13T00:00:00Z");
	}

	@Test
	@DisplayName("listener does not propagate notification creation failure")
	void handleSubscriptionExpiringSoonServiceThrowsDoesNotPropagate() {
		willThrow(new RuntimeException("DB down"))
			.given(notificationService).createIfAbsent(
				anyLong(),
				isNull(),
				any(NotificationType.class),
				anyString(),
				anyLong(),
				any(NotificationReferenceType.class),
				anyString()
			);

		assertThatCode(() -> listener.handleSubscriptionExpiringSoon(event()))
			.doesNotThrowAnyException();
	}

	private SubscriptionExpiringSoonEvent event() {
		return new SubscriptionExpiringSoonEvent(
			SUBSCRIPTION_ID,
			SUBSCRIBER_ID,
			CREATOR_ID,
			PERIOD_END_AT
		);
	}
}
