package com.white.handdam.notification.event;

import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.service.NotificationService;
import com.white.handdam.payment.event.PaymentFailedEvent;
import com.white.handdam.payment.event.PaymentSucceededEvent;
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
class PaymentNotificationListenerTest {

	private static final Long PAYMENT_ID = 60L;
	private static final Long MEMBER_ID = 1L;
	private static final String ORDER_ID = "order-1";

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private PaymentNotificationListener paymentNotificationListener;

	@Test
	@DisplayName("결제 성공 이벤트 - 결제자에게 PAYMENT_SUCCESS 알림을 만들고 senderId는 null이다")
	void handlePaymentSucceeded_success() {
		paymentNotificationListener.handlePaymentSucceeded(
			new PaymentSucceededEvent(PAYMENT_ID, MEMBER_ID, 10000, ORDER_ID));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(notificationService).create(
			eq(MEMBER_ID),
			// 시스템 알림 - 수신자를 senderId에 넣으면 자기알림 억제로 조용히 사라진다
			isNull(),
			eq(NotificationType.PAYMENT_SUCCESS),
			messageCaptor.capture(),
			eq(PAYMENT_ID),
			eq(NotificationReferenceType.PAYMENT));

		assertThat(messageCaptor.getValue()).contains("10,000");
	}

	@Test
	@DisplayName("결제 실패 이벤트 - 실패 사유가 문구에 포함된다")
	void handlePaymentFailed_success() {
		paymentNotificationListener.handlePaymentFailed(
			new PaymentFailedEvent(PAYMENT_ID, MEMBER_ID, ORDER_ID, "카드 한도 초과"));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(notificationService).create(
			eq(MEMBER_ID),
			isNull(),
			eq(NotificationType.PAYMENT_FAILED),
			messageCaptor.capture(),
			eq(PAYMENT_ID),
			eq(NotificationReferenceType.PAYMENT));

		assertThat(messageCaptor.getValue()).contains("카드 한도 초과");
	}

	@Test
	@DisplayName("결제 실패 이벤트 - 실패 사유가 비어 있으면 기본 문구를 쓴다")
	void handlePaymentFailed_blankReason_usesDefaultMessage() {
		paymentNotificationListener.handlePaymentFailed(
			new PaymentFailedEvent(PAYMENT_ID, MEMBER_ID, ORDER_ID, null));

		ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
		verify(notificationService).create(
			eq(MEMBER_ID), isNull(), eq(NotificationType.PAYMENT_FAILED),
			messageCaptor.capture(), eq(PAYMENT_ID), eq(NotificationReferenceType.PAYMENT));

		assertThat(messageCaptor.getValue()).isEqualTo("결제에 실패했습니다.");
	}

	@Test
	@DisplayName("알림 생성이 실패해도 예외가 전파되지 않는다 (결제 트랜잭션 보호)")
	void handlePaymentSucceeded_serviceThrows_doesNotPropagate() {
		willThrow(new RuntimeException("DB down"))
			.given(notificationService).create(
				anyLong(), isNull(), any(NotificationType.class), anyString(), anyLong(),
				any(NotificationReferenceType.class));

		assertThatCode(() -> paymentNotificationListener.handlePaymentSucceeded(
			new PaymentSucceededEvent(PAYMENT_ID, MEMBER_ID, 10000, ORDER_ID)))
			.doesNotThrowAnyException();
	}
}
