package com.white.handdam.notification.service;

import com.white.handdam.notification.dto.response.NotificationResponse;
import com.white.handdam.notification.entity.NotificationEntity;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.repository.NotificationRepository;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceDedupTest {

	private static final Long RECIPIENT_ID = 1L;
	private static final Long SENDER_ID = 2L;
	private static final Long SUBSCRIPTION_ID = 10L;
	private static final String DEDUP_KEY = "SUBSCRIPTION_EXPIRING:10:2026-08-13T00:00:00Z";

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private NotificationPublisher notificationPublisher;

	@InjectMocks
	private NotificationService notificationService;

	@Test
	@DisplayName("createIfAbsent keeps existing create path when dedupKey is null")
	void createIfAbsentWithoutDedupKeyUsesExistingCreatePath() {
		given(notificationRepository.save(any(NotificationEntity.class)))
			.willReturn(notification(null));

		boolean created = notificationService.createIfAbsent(
			RECIPIENT_ID,
			SENDER_ID,
			NotificationType.CHAT_MESSAGE,
			"message",
			1L,
			NotificationReferenceType.CHAT_ROOM,
			null
		);

		assertThat(created).isTrue();
		verify(notificationRepository).save(any(NotificationEntity.class));
		verify(notificationRepository, never()).saveAndFlush(any(NotificationEntity.class));
	}

	@Test
	@DisplayName("createIfAbsent saves notification with dedupKey")
	void createIfAbsentWithDedupKeyCreatesNotification() {
		given(notificationRepository.saveAndFlush(any(NotificationEntity.class)))
			.willReturn(notification(DEDUP_KEY));

		boolean created = notificationService.createIfAbsent(
			RECIPIENT_ID,
			null,
			NotificationType.SUBSCRIPTION_EXPIRING,
			"subscription expiring",
			SUBSCRIPTION_ID,
			NotificationReferenceType.SUBSCRIPTION,
			DEDUP_KEY
		);

		ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
		verify(notificationRepository).saveAndFlush(captor.capture());
		assertThat(created).isTrue();
		assertThat(captor.getValue().getDedupKey()).isEqualTo(DEDUP_KEY);
		verify(notificationPublisher).publish(eq(RECIPIENT_ID), any(NotificationResponse.class));
	}

	@Test
	@DisplayName("createIfAbsent treats dedupKey unique violation as idempotent duplicate")
	void createIfAbsentDuplicateDedupKeyReturnsFalse() {
		given(notificationRepository.saveAndFlush(any(NotificationEntity.class)))
			.willThrow(dataIntegrityViolation("uk_notification_dedup_key"));

		boolean created = notificationService.createIfAbsent(
			RECIPIENT_ID,
			null,
			NotificationType.SUBSCRIPTION_EXPIRING,
			"subscription expiring",
			SUBSCRIPTION_ID,
			NotificationReferenceType.SUBSCRIPTION,
			DEDUP_KEY
		);

		assertThat(created).isFalse();
		verify(notificationPublisher, never()).publish(anyLong(), any(NotificationResponse.class));
	}

	@Test
	@DisplayName("createIfAbsent does not hide unrelated integrity violations")
	void createIfAbsentOtherIntegrityViolationPropagates() {
		given(notificationRepository.saveAndFlush(any(NotificationEntity.class)))
			.willThrow(dataIntegrityViolation("other_constraint"));

		assertThatThrownBy(() -> notificationService.createIfAbsent(
			RECIPIENT_ID,
			null,
			NotificationType.SUBSCRIPTION_EXPIRING,
			"subscription expiring",
			SUBSCRIPTION_ID,
			NotificationReferenceType.SUBSCRIPTION,
			DEDUP_KEY
		)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private NotificationEntity notification(String dedupKey) {
		NotificationEntity notification = NotificationEntity.builder()
			.memberId(RECIPIENT_ID)
			.senderId(null)
			.type(NotificationType.SUBSCRIPTION_EXPIRING)
			.message("subscription expiring")
			.referenceId(SUBSCRIPTION_ID)
			.referenceType(NotificationReferenceType.SUBSCRIPTION.name())
			.dedupKey(dedupKey)
			.build();
		ReflectionTestUtils.setField(notification, "id", 100L);
		ReflectionTestUtils.setField(notification, "createdAt", Instant.parse("2026-08-10T00:00:00Z"));
		return notification;
	}

	private DataIntegrityViolationException dataIntegrityViolation(String constraintName) {
		return new DataIntegrityViolationException(
			constraintName,
			new ConstraintViolationException(constraintName, new SQLException(), constraintName)
		);
	}
}
