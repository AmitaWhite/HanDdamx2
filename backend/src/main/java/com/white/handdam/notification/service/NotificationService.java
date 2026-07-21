package com.white.handdam.notification.service;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.notification.converter.NotificationConverter;
import com.white.handdam.notification.dto.response.NotificationReadAllResponse;
import com.white.handdam.notification.dto.response.NotificationResponse;
import com.white.handdam.notification.entity.NotificationEntity;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.exception.NotificationErrorCode;
import com.white.handdam.notification.repository.NotificationRepository;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	public static final int MAX_MESSAGE_LENGTH = 500;
	private static final String DEDUP_KEY_CONSTRAINT_NAME = "uk_notification_dedup_key";

	private final NotificationRepository notificationRepository;
	private final NotificationPublisher notificationPublisher;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void create(
		Long memberId,
		Long senderId,
		NotificationType type,
		String message,
		Long referenceId,
		NotificationReferenceType referenceType
	) {
		if (memberId == null || memberId.equals(senderId)) {
			return;
		}

		NotificationEntity saved = notificationRepository.save(buildNotification(
			memberId,
			senderId,
			type,
			message,
			referenceId,
			referenceType,
			null
		));

		NotificationResponse response = NotificationConverter.toResponse(saved);
		publishAfterCommit(memberId, response);
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public boolean createIfAbsent(
		Long memberId,
		Long senderId,
		NotificationType type,
		String message,
		Long referenceId,
		NotificationReferenceType referenceType,
		String dedupKey
	) {
		if (memberId == null || memberId.equals(senderId)) {
			return false;
		}
		if (dedupKey == null || dedupKey.isBlank()) {
			create(memberId, senderId, type, message, referenceId, referenceType);
			return true;
		}

		try {
			NotificationEntity saved = notificationRepository.saveAndFlush(buildNotification(
				memberId,
				senderId,
				type,
				message,
				referenceId,
				referenceType,
				dedupKey
			));
			NotificationResponse response = NotificationConverter.toResponse(saved);
			publishAfterCommit(memberId, response);
			return true;
		} catch (DataIntegrityViolationException exception) {
			if (isDedupKeyConstraintViolation(exception)) {
				return false;
			}
			throw exception;
		}
	}

	private void publishAfterCommit(Long memberId, NotificationResponse response) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publishQuietly(memberId, response);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publishQuietly(memberId, response);
			}
		});
	}

	private void publishQuietly(Long memberId, NotificationResponse response) {
		try {
			notificationPublisher.publish(memberId, response);
		} catch (Exception e) {
			log.warn("Notification realtime publish failed after save. notificationId={}, memberId={}",
				response.id(), memberId, e);
		}
	}

	@Transactional(readOnly = true)
	public Slice<NotificationResponse> getMyNotifications(Long memberId, Pageable pageable) {
		return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable)
			.map(notification -> NotificationConverter.toResponse(notification));
	}

	@Transactional(readOnly = true)
	public long countUnread(Long memberId) {
		return notificationRepository.countByMemberIdAndIsReadFalse(memberId);
	}

	@Transactional
	public void markAsRead(Long notificationId, Long memberId) {
		NotificationEntity notification = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

		if (!notification.getMemberId().equals(memberId)) {
			throw new CustomException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
		}
		notification.markAsRead();
	}

	@Transactional
	public NotificationReadAllResponse markAllAsRead(Long memberId) {
		return new NotificationReadAllResponse(notificationRepository.markAllAsRead(memberId));
	}

	private NotificationEntity buildNotification(
		Long memberId,
		Long senderId,
		NotificationType type,
		String message,
		Long referenceId,
		NotificationReferenceType referenceType,
		String dedupKey
	) {
		return NotificationEntity.builder()
			.memberId(memberId)
			.senderId(senderId)
			.type(type)
			.message(truncate(message))
			.referenceId(referenceId)
			.referenceType(referenceType.name())
			.dedupKey(dedupKey)
			.build();
	}

	private static String truncate(String message) {
		if (message == null || message.length() <= MAX_MESSAGE_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_MESSAGE_LENGTH);
	}

	private boolean isDedupKeyConstraintViolation(DataIntegrityViolationException exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof ConstraintViolationException constraintViolationException
				&& containsDedupKeyConstraintName(constraintViolationException.getConstraintName())) {
				return true;
			}
			if (containsDedupKeyConstraintName(current.getMessage())) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private boolean containsDedupKeyConstraintName(String value) {
		return value != null
			&& value.toLowerCase(Locale.ROOT).contains(DEDUP_KEY_CONSTRAINT_NAME);
	}
}
