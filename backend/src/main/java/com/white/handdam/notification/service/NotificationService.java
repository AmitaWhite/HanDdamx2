package com.white.handdam.notification.service;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.notification.converter.NotificationConverter;
import com.white.handdam.notification.dto.response.NotificationReadAllResponse;
import com.white.handdam.notification.dto.response.NotificationResponse;
import com.white.handdam.notification.entity.NotificationEntity;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.exception.NotificationErrorCode;
import com.white.handdam.notification.repository.NotificationRepository;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	/** 알림 본문 최대 길이 — notification.message 컬럼이 VARCHAR(500) */
	public static final int MAX_MESSAGE_LENGTH = 500;

	private final NotificationRepository notificationRepository;
	private final NotificationPublisher notificationPublisher;

	/**
	 * 알림 저장 + 실시간 전송.
	 *
	 * <p>저장이 본체고 전송은 부가 기능이다. 미접속 사용자는 전송이 스킵될 뿐 알림은 남아야 하므로,
	 * 전송 실패가 저장을 롤백시키지 않도록 전송을 try/catch 로 감싼다.
	 *
	 * <p>{@code REQUIRES_NEW} 인 이유: 호출자인 리스너가
	 * {@code @TransactionalEventListener(AFTER_COMMIT)} 이라 원본 트랜잭션이 이미 끝난 뒤 실행된다.
	 * 현재는 {@code @Async} 로 별도 스레드에서 돌아 트랜잭션 컨텍스트 자체가 없으므로 기본 전파도 새 트랜잭션을 열지만,
	 * 나중에 {@code @Async} 가 제거되어도 안전하도록 명시해 둔다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void create(
		Long memberId,
		Long senderId,
		NotificationType type,
		String message,
		Long referenceId,
		String referenceType
	) {
		// 내 행동으로 나에게 알림이 가지 않도록 (주 정책은 리스너, 여기는 방어선)
		if (memberId == null || memberId.equals(senderId)) {
			return;
		}

		NotificationEntity saved = notificationRepository.save(
			NotificationEntity.builder()
				.memberId(memberId)
				.senderId(senderId)
				.type(type)
				.message(truncate(message))
				.referenceId(referenceId)
				.referenceType(referenceType)
				.build()
		);

		// 전송 실패가 저장을 되돌리면 안 된다
		try {
			notificationPublisher.publish(memberId, NotificationConverter.toResponse(saved));
		} catch (Exception e) {
			log.warn("알림 실시간 전송 실패(저장은 완료): notificationId={}, memberId={}",
				saved.getId(), memberId, e);
		}
	}

	/** 내 알림 목록 (최신순, 무한 스크롤) */
	@Transactional(readOnly = true)
	public Slice<NotificationResponse> getMyNotifications(Long memberId, Pageable pageable) {
		return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
			.map(NotificationConverter::toResponse);
	}

	/** 안 읽은 알림 개수 */
	@Transactional(readOnly = true)
	public long countUnread(Long memberId) {
		return notificationRepository.countByMemberIdAndIsReadFalse(memberId);
	}

	/**
	 * 알림 읽음 처리.
	 *
	 * @throws CustomException 알림 없음({@link NotificationErrorCode#NOTIFICATION_NOT_FOUND}),
	 *                         본인 알림 아님({@link NotificationErrorCode#NOTIFICATION_FORBIDDEN})
	 */
	@Transactional
	public void markAsRead(Long notificationId, Long memberId) {
		NotificationEntity notification = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

		if (!notification.getMemberId().equals(memberId)) {
			throw new CustomException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
		}
		// 더티 체킹으로 반영 (명시적 save 불필요)
		notification.markAsRead();
	}

	/**
	 * 내 알림 전체 읽음 처리.
	 *
	 * <p>쿼리에 {@code memberId} 조건이 있어 본인 알림만 갱신되므로 소유권 검사가 따로 필요 없다.
	 */
	@Transactional
	public NotificationReadAllResponse markAllAsRead(Long memberId) {
		return new NotificationReadAllResponse(notificationRepository.markAllAsRead(memberId));
	}

	/** message 컬럼 길이(500)를 넘지 않도록 자른다. */
	private static String truncate(String message) {
		if (message == null || message.length() <= MAX_MESSAGE_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_MESSAGE_LENGTH);
	}
}
