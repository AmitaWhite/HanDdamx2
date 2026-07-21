package com.white.handdam.notification.event;

import com.white.handdam.creator.event.CreatorApplicationApprovedEvent;
import com.white.handdam.creator.event.CreatorApplicationRejectedEvent;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreatorNotificationListenerTest {

	private static final Long APPLICATION_ID = 70L;
	private static final Long APPLICANT_ID = 1L;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private CreatorNotificationListener creatorNotificationListener;

	@Test
	@DisplayName("심사 승인 이벤트 - 신청자에게 알림을 만들고 senderId는 null이다 (관리자 ID 비노출)")
	void handleApplicationApproved_success() {
		creatorNotificationListener.handleApplicationApproved(
			new CreatorApplicationApprovedEvent(APPLICATION_ID, APPLICANT_ID));

		verify(notificationService).create(
			eq(APPLICANT_ID),
			isNull(),
			eq(NotificationType.CREATOR_APPLICATION_APPROVED),
			anyString(),
			eq(APPLICATION_ID),
			eq(NotificationReferenceType.CREATOR_APPLICATION));
	}

	@Test
	@DisplayName("심사 거절 이벤트 - 거절 사유가 문구에 포함된다")
	void handleApplicationRejected_success() {
		creatorNotificationListener.handleApplicationRejected(
			new CreatorApplicationRejectedEvent(APPLICATION_ID, APPLICANT_ID, "포트폴리오 부족"));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(notificationService).create(
			eq(APPLICANT_ID),
			isNull(),
			eq(NotificationType.CREATOR_APPLICATION_REJECTED),
			messageCaptor.capture(),
			eq(APPLICATION_ID),
			eq(NotificationReferenceType.CREATOR_APPLICATION));

		assertThat(messageCaptor.getValue()).contains("포트폴리오 부족");
	}

	@Test
	@DisplayName("심사 거절 이벤트 - 사유가 비어 있으면 기본 문구를 쓴다")
	void handleApplicationRejected_blankReason_usesDefaultMessage() {
		creatorNotificationListener.handleApplicationRejected(
			new CreatorApplicationRejectedEvent(APPLICATION_ID, APPLICANT_ID, "  "));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(notificationService).create(
			eq(APPLICANT_ID), isNull(), eq(NotificationType.CREATOR_APPLICATION_REJECTED),
			messageCaptor.capture(), eq(APPLICATION_ID),
			eq(NotificationReferenceType.CREATOR_APPLICATION));

		assertThat(messageCaptor.getValue()).isEqualTo("크리에이터 전환 신청이 거절되었습니다.");
	}

	@Test
	@DisplayName("알림 생성이 실패해도 예외가 전파되지 않는다 (심사 트랜잭션 보호)")
	void handleApplicationApproved_serviceThrows_doesNotPropagate() {
		willThrow(new RuntimeException("DB down"))
			.given(notificationService).create(
				anyLong(), isNull(), any(NotificationType.class), anyString(), anyLong(),
				any(NotificationReferenceType.class));

		assertThatCode(() -> creatorNotificationListener.handleApplicationApproved(
			new CreatorApplicationApprovedEvent(APPLICATION_ID, APPLICANT_ID)))
			.doesNotThrowAnyException();
	}
}
