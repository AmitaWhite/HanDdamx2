package com.white.handdam.notification.event;

import com.white.handdam.chat.entity.ChatMessageType;
import com.white.handdam.chat.event.ChatMessageSentEvent;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 리스너 메서드를 직접 호출해 "번역" 로직만 검증한다.
 * {@code @Async}/{@code @TransactionalEventListener} 동작은 검증 대상이 아니다
 * (VerificationEmailEventListener 도 동일한 방식).
 */
@ExtendWith(MockitoExtension.class)
class ChatNotificationListenerTest {

	private static final Long CHAT_ROOM_ID = 42L;
	private static final Long MESSAGE_ID = 7L;
	private static final Long SENDER_ID = 2L;
	private static final Long RECIPIENT_ID = 1L;
	private static final NotificationReferenceType REFERENCE_TYPE_CHAT_ROOM =
		NotificationReferenceType.CHAT_ROOM;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private ChatNotificationListener chatNotificationListener;

	@Test
	@DisplayName("채팅 메시지 이벤트 - 수신자에게 CHAT_MESSAGE 알림을 생성한다")
	void handleChatMessageSent_success() {
		chatNotificationListener.handleChatMessageSent(sampleEvent(RECIPIENT_ID, "안녕하세요"));

		ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<Long> senderIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Long> referenceIdCaptor = ArgumentCaptor.forClass(Long.class);
		ArgumentCaptor<NotificationReferenceType> referenceTypeCaptor =
			ArgumentCaptor.forClass(NotificationReferenceType.class);

		verify(notificationService).create(
			memberIdCaptor.capture(),
			senderIdCaptor.capture(),
			typeCaptor.capture(),
			messageCaptor.capture(),
			referenceIdCaptor.capture(),
			referenceTypeCaptor.capture()
		);

		assertThat(memberIdCaptor.getValue()).isEqualTo(RECIPIENT_ID);
		assertThat(senderIdCaptor.getValue()).isEqualTo(SENDER_ID);
		assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.CHAT_MESSAGE);
		assertThat(messageCaptor.getValue()).isEqualTo("안녕하세요");
		assertThat(referenceIdCaptor.getValue()).isEqualTo(CHAT_ROOM_ID);
		assertThat(referenceTypeCaptor.getValue()).isEqualTo(REFERENCE_TYPE_CHAT_ROOM);
	}

	@Test
	@DisplayName("채팅 메시지 이벤트 - 수신자가 없으면 알림을 생성하지 않는다")
	void handleChatMessageSent_noRecipient_skipped() {
		chatNotificationListener.handleChatMessageSent(sampleEvent(null, "안녕하세요"));

		verify(notificationService, never()).create(
			anyLong(), anyLong(), any(NotificationType.class), anyString(), anyLong(),
			any(NotificationReferenceType.class));
	}

	@Test
	@DisplayName("채팅 메시지 이벤트 - 알림 생성이 실패해도 예외가 전파되지 않는다 (원본 도메인 보호)")
	void handleChatMessageSent_serviceThrows_doesNotPropagate() {
		willThrow(new RuntimeException("DB down"))
			.given(notificationService).create(
				anyLong(), anyLong(), any(NotificationType.class), anyString(), anyLong(),
			any(NotificationReferenceType.class));

		assertThatCode(() ->
			chatNotificationListener.handleChatMessageSent(sampleEvent(RECIPIENT_ID, "안녕하세요")))
			.doesNotThrowAnyException();
	}

	private ChatMessageSentEvent sampleEvent(Long recipientId, String contentPreview) {
		return new ChatMessageSentEvent(
			CHAT_ROOM_ID,
			MESSAGE_ID,
			SENDER_ID,
			recipientId,
			ChatMessageType.TEXT,
			contentPreview
		);
	}
}
