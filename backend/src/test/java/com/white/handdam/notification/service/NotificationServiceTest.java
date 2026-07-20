package com.white.handdam.notification.service;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.notification.dto.response.NotificationReadAllResponse;
import com.white.handdam.notification.dto.response.NotificationResponse;
import com.white.handdam.notification.entity.NotificationEntity;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.exception.NotificationErrorCode;
import com.white.handdam.notification.repository.NotificationRepository;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	private static final Long RECIPIENT_ID = 1L;
	private static final Long SENDER_ID = 2L;
	private static final Long CHAT_ROOM_ID = 42L;
	private static final Long NOTIFICATION_ID = 100L;
	private static final NotificationReferenceType REFERENCE_TYPE_CHAT_ROOM =
		NotificationReferenceType.CHAT_ROOM;
	private static final Instant CREATED_AT = Instant.parse("2026-07-20T00:00:00Z");

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private NotificationPublisher notificationPublisher;

	@InjectMocks
	private NotificationService notificationService;

	// ---------------------------------------------------------------
	// 알림 생성
	// ---------------------------------------------------------------

	@Test
	@DisplayName("알림 생성 성공 - 저장 후 실시간 전송까지 수행한다")
	void create_success() {
		given(notificationRepository.save(any(NotificationEntity.class)))
			.willReturn(sampleNotification(RECIPIENT_ID, false));

		notificationService.create(RECIPIENT_ID, SENDER_ID, NotificationType.CHAT_MESSAGE,
			"안녕하세요", CHAT_ROOM_ID, REFERENCE_TYPE_CHAT_ROOM);

		ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
		verify(notificationRepository).save(captor.capture());
		NotificationEntity saved = captor.getValue();
		assertThat(saved.getMemberId()).isEqualTo(RECIPIENT_ID);
		assertThat(saved.getSenderId()).isEqualTo(SENDER_ID);
		assertThat(saved.getType()).isEqualTo(NotificationType.CHAT_MESSAGE);
		assertThat(saved.getReferenceId()).isEqualTo(CHAT_ROOM_ID);
		assertThat(saved.getReferenceType()).isEqualTo(REFERENCE_TYPE_CHAT_ROOM.name());
		assertThat(saved.getIsRead()).isFalse();

		verify(notificationPublisher).publish(eq(RECIPIENT_ID), any(NotificationResponse.class));
	}

	@Test
	@DisplayName("알림 생성 - 자기 자신이 유발한 알림은 저장하지 않는다")
	void create_selfNotification_skipped() {
		notificationService.create(RECIPIENT_ID, RECIPIENT_ID, NotificationType.CHAT_MESSAGE,
			"내가 보낸 메시지", CHAT_ROOM_ID, REFERENCE_TYPE_CHAT_ROOM);

		verify(notificationRepository, never()).save(any(NotificationEntity.class));
		verify(notificationPublisher, never()).publish(anyLong(), any(NotificationResponse.class));
	}

	@Test
	@DisplayName("알림 생성 - 실시간 전송이 실패해도 저장은 유지되고 예외가 전파되지 않는다")
	void create_publishFails_stillSaved() {
		given(notificationRepository.save(any(NotificationEntity.class)))
			.willReturn(sampleNotification(RECIPIENT_ID, false));
		willThrow(new RuntimeException("WebSocket down"))
			.given(notificationPublisher).publish(anyLong(), any(NotificationResponse.class));

		assertThatCode(() -> notificationService.create(RECIPIENT_ID, SENDER_ID,
			NotificationType.CHAT_MESSAGE, "안녕하세요", CHAT_ROOM_ID, REFERENCE_TYPE_CHAT_ROOM))
			.doesNotThrowAnyException();

		verify(notificationRepository).save(any(NotificationEntity.class));
	}

	@Test
	@DisplayName("알림 생성 - message가 500자를 넘으면 잘라서 저장한다")
	void create_longMessage_truncated() {
		given(notificationRepository.save(any(NotificationEntity.class)))
			.willReturn(sampleNotification(RECIPIENT_ID, false));
		String tooLong = "가".repeat(600);

		notificationService.create(RECIPIENT_ID, SENDER_ID, NotificationType.CHAT_MESSAGE,
			tooLong, CHAT_ROOM_ID, REFERENCE_TYPE_CHAT_ROOM);

		ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
		verify(notificationRepository).save(captor.capture());
		assertThat(captor.getValue().getMessage())
			.hasSize(NotificationService.MAX_MESSAGE_LENGTH);
	}

	@Test
	@DisplayName("알림 생성 - 트랜잭션 동기화가 있으면 커밋 이후에 전송한다")
	void create_publishDeferredUntilAfterCommit() {
		given(notificationRepository.save(any(NotificationEntity.class)))
			.willReturn(sampleNotification(RECIPIENT_ID, false));

		TransactionSynchronizationManager.initSynchronization();
		try {
			notificationService.create(RECIPIENT_ID, SENDER_ID, NotificationType.CHAT_MESSAGE,
				"안녕하세요", CHAT_ROOM_ID, REFERENCE_TYPE_CHAT_ROOM);

			// 저장은 됐지만 아직 커밋 전이므로 전송되지 않아야 한다
			verify(notificationRepository).save(any(NotificationEntity.class));
			verify(notificationPublisher, never()).publish(anyLong(), any(NotificationResponse.class));

			TransactionSynchronizationUtils.triggerAfterCommit();

			verify(notificationPublisher).publish(eq(RECIPIENT_ID), any(NotificationResponse.class));
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	// ---------------------------------------------------------------
	// 조회
	// ---------------------------------------------------------------

	@Test
	@DisplayName("내 알림 목록 조회 성공 - 응답 DTO로 매핑되고 hasNext가 유지된다")
	void getMyNotifications_success() {
		Pageable pageable = PageRequest.of(0, 2);
		Slice<NotificationEntity> slice = new SliceImpl<>(
			List.of(sampleNotification(RECIPIENT_ID, false)), pageable, true);
		given(notificationRepository.findByMemberIdOrderByCreatedAtDesc(RECIPIENT_ID, pageable))
			.willReturn(slice);

		Slice<NotificationResponse> result =
			notificationService.getMyNotifications(RECIPIENT_ID, pageable);

		assertThat(result.hasNext()).isTrue();
		assertThat(result.getContent()).hasSize(1);
		NotificationResponse response = result.getContent().getFirst();
		assertThat(response.id()).isEqualTo(NOTIFICATION_ID);
		assertThat(response.type()).isEqualTo(NotificationType.CHAT_MESSAGE);
		assertThat(response.referenceId()).isEqualTo(CHAT_ROOM_ID);
		assertThat(response.isRead()).isFalse();
		assertThat(response.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	@DisplayName("안 읽은 알림 개수 조회 성공")
	void countUnread_success() {
		given(notificationRepository.countByMemberIdAndIsReadFalse(RECIPIENT_ID)).willReturn(3L);

		assertThat(notificationService.countUnread(RECIPIENT_ID)).isEqualTo(3L);
	}

	// ---------------------------------------------------------------
	// 읽음 처리
	// ---------------------------------------------------------------

	@Test
	@DisplayName("알림 읽음 처리 성공 - isRead가 true로 바뀐다")
	void markAsRead_success() {
		NotificationEntity notification = sampleNotification(RECIPIENT_ID, false);
		given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

		notificationService.markAsRead(NOTIFICATION_ID, RECIPIENT_ID);

		assertThat(notification.getIsRead()).isTrue();
	}

	@Test
	@DisplayName("알림 읽음 처리 실패 - 존재하지 않는 알림")
	void markAsRead_notFound() {
		given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.empty());

		assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, RECIPIENT_ID))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode())
				.isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
	}

	@Test
	@DisplayName("알림 읽음 처리 실패 - 본인 알림이 아니면 읽음 처리할 수 없다")
	void markAsRead_notOwner() {
		NotificationEntity notification = sampleNotification(RECIPIENT_ID, false);
		given(notificationRepository.findById(NOTIFICATION_ID)).willReturn(Optional.of(notification));

		assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, SENDER_ID))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException) e).getErrorCode())
				.isEqualTo(NotificationErrorCode.NOTIFICATION_FORBIDDEN));
		assertThat(notification.getIsRead()).isFalse();
	}

	@Test
	@DisplayName("알림 전체 읽음 처리 성공 - 갱신 건수를 반환한다")
	void markAllAsRead_success() {
		given(notificationRepository.markAllAsRead(RECIPIENT_ID)).willReturn(3);

		NotificationReadAllResponse response = notificationService.markAllAsRead(RECIPIENT_ID);

		assertThat(response.updatedCount()).isEqualTo(3L);
	}

	// ---------------------------------------------------------------
	// fixture
	// ---------------------------------------------------------------

	/**
	 * 단위 테스트에서는 persist 가 없어 id/createdAt 이 채워지지 않으므로
	 * ReflectionTestUtils 로 주입한다. createdAt 은 상위 클래스(BaseCreatedAtEntity) 필드다.
	 */
	private NotificationEntity sampleNotification(Long memberId, boolean read) {
		NotificationEntity notification = NotificationEntity.builder()
			.memberId(memberId)
			.senderId(SENDER_ID)
			.type(NotificationType.CHAT_MESSAGE)
			.message("안녕하세요")
			.referenceId(CHAT_ROOM_ID)
			.referenceType(REFERENCE_TYPE_CHAT_ROOM.name())
			.build();
		ReflectionTestUtils.setField(notification, "id", NOTIFICATION_ID);
		ReflectionTestUtils.setField(notification, "createdAt", CREATED_AT);
		if (read) {
			notification.markAsRead();
		}
		return notification;
	}
}
