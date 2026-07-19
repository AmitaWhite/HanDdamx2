package com.white.handdam.notification.event;

import com.white.handdam.chat.event.ChatMessageSentEvent;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 도메인 이벤트를 구독해 알림으로 변환한다.
 *
 * 리스너의 책임은 "번역" 하나다 — 도메인 이벤트를 '누구에게 갈 알림인가'로 바꾼다.
 * 저장·전송은 {@link NotificationService} 에 위임한다.
 *
 * 각 핸들러의 3중 안전장치:
 * 
 * {@code AFTER_COMMIT} — 원본 트랜잭션이 롤백되면 실행되지 않아 유령 알림을 막는다</li>
 * {@code @Async} — 별도 스레드로 빠져 알림 처리가 원본 응답을 지연시키지 않는다</li>
 * {@code try/catch} — 알림 실패가 원본 도메인 동작을 깨뜨리지 않는다</li>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

	/** 채팅 알림의 다형 참조 타입 — 클릭 시 채팅방으로 이동 */
	private static final String REFERENCE_TYPE_CHAT_ROOM = "CHAT_ROOM";

	private final NotificationService notificationService;

	/**
	 * 채팅 메시지 수신 알림.
	 *
	 * 수신자({@code recipientId})는 {@code ChatMessageService} 가 미리 계산해 이벤트에 담아준다.
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
					REFERENCE_TYPE_CHAT_ROOM);
		} catch (Exception e) {
			// 알림 실패가 채팅 메시지 전송을 깨뜨리면 안 된다
			log.error("채팅 알림 생성 실패: messageId={}, recipientId={}",
					event.messageId(), event.recipientId(), e);
		}
	}
}
