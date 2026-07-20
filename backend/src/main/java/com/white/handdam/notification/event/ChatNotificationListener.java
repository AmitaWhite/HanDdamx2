package com.white.handdam.notification.event;

import com.white.handdam.chat.event.ChatMessageSentEvent;
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
 * 채팅 도메인 이벤트를 알림으로 변환한다.
 *
 * <p>리스너의 책임은 "번역" 하나다 — 도메인 이벤트를 '누구에게 갈 알림인가'로 바꾼다.
 * 저장·전송은 {@link NotificationService} 에 위임한다.
 *
 * <p>3중 안전장치({@code AFTER_COMMIT} · {@code @Async} · {@code try/catch})의 이유는
 * 이 패키지의 {@code package-info.java} 에 정리되어 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationListener {

	private final NotificationService notificationService;

	/**
	 * 채팅 메시지 수신 알림.
	 *
	 * <p>수신자({@code recipientId})는 {@code ChatMessageService} 가 미리 계산해 이벤트에 담아준다.
	 */
	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleChatMessageSent(ChatMessageSentEvent event) {
		try {
			if (event.recipientId() == null) {
				log.warn("채팅 알림 대상 없음: messageId={}, chatRoomId={}",
					event.messageId(), event.chatRoomId());
				return;
			}

			notificationService.create(
				event.recipientId(),
				event.senderId(),
				NotificationType.CHAT_MESSAGE,
				event.contentPreview(),
				event.chatRoomId(),
				NotificationReferenceType.CHAT_ROOM);
		} catch (Exception e) {
			// 알림 실패가 채팅 메시지 전송을 깨뜨리면 안 된다
			log.error("채팅 알림 생성 실패: messageId={}, recipientId={}",
				event.messageId(), event.recipientId(), e);
		}
	}
}
