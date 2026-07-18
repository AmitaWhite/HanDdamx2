package com.white.handdam.chat.service;

import com.white.handdam.board.service.PaidSubscriptionChecker;
import com.white.handdam.chat.converter.ChatMessageConverter;
import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.dto.response.ChatReadResponse;
import com.white.handdam.chat.entity.ChatMessage;
import com.white.handdam.chat.entity.ChatMessageType;
import com.white.handdam.chat.entity.ChatRoom;
import com.white.handdam.chat.event.ChatMessageSentEvent;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.repository.ChatMessageRepository;
import com.white.handdam.chat.websocket.publisher.ChatMessagePublisher;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

	/** 이미지 메시지의 알림 미리보기 문구 (본문이 없으므로 고정 문구 사용). */
	private static final String IMAGE_PREVIEW = "[이미지]";

	private final ChatRoomService chatRoomService;
	private final ChatMessageRepository chatMessageRepository;
	private final PaidSubscriptionChecker paidSubscriptionChecker;
	private final ObjectStorage objectStorage;
	private final ChatMessagePublisher chatMessagePublisher;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * 채팅 메시지 내역 조회 (CHAT-006 / LDJ-020).
	 *
	 * <pre>
	 * 1. 로그인·방 존재·참여자 확인 ({@link ChatRoomService#requireParticipatingRoom})
	 * 2. sentAt 기준 페이지 조회 (구독 만료·CLOSED 여도 조회 허용)
	 * </pre>
	 */
	public Page<ChatMessageResponse> getMessages(Long chatRoomId, Long memberId, Pageable pageable) {
		chatRoomService.requireParticipatingRoom(chatRoomId, memberId);

		return chatMessageRepository.findByChatRoomId(chatRoomId, pageable)
			.map(ChatMessageConverter::toResponse);
	}

	/**
	 * 텍스트·이미지 메시지 전송 (CHAT-002~004 / LDJ-021).
	 *
	 * <pre>
	 * 1. 로그인·방 존재·참여자 확인
	 * 2. ACTIVE 방에서만 전송 (CLOSED → 403)
	 * 3. 구독자(member)는 활성 유료 구독 필요 / 크리에이터(작성자)는 구독 검사 생략
	 * 4. TEXT: content 필수 / IMAGE: image 파일 필수 → S3 업로드
	 * 5. 메시지 저장 후 chat_room.last_message_at 갱신
	 * 6. WebSocket 구독자(/sub/chat-rooms/{id})에게 브로드캐스트
	 * 7. ChatMessageSentEvent 발행 (Phase 1: Spring Application Event, 오프라인 알림용)
	 * </pre>
	 */
	@Transactional
	public ChatMessageResponse sendMessage(
		Long chatRoomId,
		Long senderId,
		ChatMessageType type,
		String content,
		MultipartFile image
	) {
		ChatRoom room = chatRoomService.requireParticipatingRoom(chatRoomId, senderId);
		assertCanSend(room, senderId);

		if (type == null) {
			throw new CustomException(ChatErrorCode.CHAT_MESSAGE_INVALID);
		}

		ChatMessage message = switch (type) {
			//텍스트 메시지 생성
			case TEXT -> buildTextMessage(room.getId(), senderId, content);
			//이미지 메시지 생성
			case IMAGE -> buildImageMessage(room.getId(), senderId, image);
		};
		
		ChatMessage saved = chatMessageRepository.save(message);
		//방의 마지막 메시지 시간 업데이트
		room.updateLastMessageAt(saved.getSentAt());

		ChatMessageResponse response = ChatMessageConverter.toResponse(saved);
		chatMessagePublisher.publish(room.getId(), response);

		// TODO(NOTIFICATION): ChatMessageSentEvent 구독 리스너에서 알림 저장·전송 처리 (현재는 발행만)
		eventPublisher.publishEvent(new ChatMessageSentEvent(
			room.getId(),
			saved.getId(),
			senderId,
			resolveRecipientId(room, senderId),
			saved.getType(),
			buildContentPreview(saved)
		));
		return response;
	}

	/**
	 * 상대방 미확인 메시지 일괄 읽음 (CHAT-007 / LDJ-022).
	 *
	 * <pre>
	 * 1. 로그인·방 존재·참여자 확인
	 * 2. 상대(senderId ≠ reader)가 보낸 readAt IS NULL 메시지를 일괄 갱신
	 * 3. CLOSED·구독 만료여도 읽음 처리 허용 (조회와 동일)
	 * </pre>
	 */
	@Transactional
	public ChatReadResponse markMessagesAsRead(Long chatRoomId, Long readerId) {
		chatRoomService.requireParticipatingRoom(chatRoomId, readerId);

		int updated = chatMessageRepository.markOpponentMessagesAsRead(
			chatRoomId,
			readerId,
			Instant.now()
		);
		return new ChatReadResponse(updated);
	}

	/**
	 * 전송 권한: ACTIVE + (크리에이터이거나 활성 유료 구독자).
	 */
	private void assertCanSend(ChatRoom room, Long senderId) {
		if (!room.isActive()) {
			throw new CustomException(ChatErrorCode.CHAT_ROOM_CLOSED);
		}
		// 구독자 측만 유료 구독 검사. 크리에이터(작성자)는 전송 가능.
		if (room.isSubscriber(senderId)
			&& !paidSubscriptionChecker.hasActivePaidSubscription(senderId, room.getCreatorId())) {
			throw new CustomException(ChatErrorCode.CHAT_MESSAGE_SEND_SUBSCRIPTION_REQUIRED);
		}
	}

	private ChatMessage buildTextMessage(Long chatRoomId, Long senderId, String content) {
		//의미 있는글자가없으면(공백만 있으면) 예외 발생
		if (!StringUtils.hasText(content)) {
			throw new CustomException(ChatErrorCode.CHAT_MESSAGE_INVALID);
		}
		//빈/공백만인 문자열 예외 발생
		//2000자 초과 예외 발생
		String trimmed = content.trim();
		if (trimmed.length() > 2000) {
			throw new CustomException(ChatErrorCode.CHAT_MESSAGE_INVALID);
		}
		return ChatMessage.builder()
			.chatRoomId(chatRoomId)
			.senderId(senderId)
			.type(ChatMessageType.TEXT)
			.content(trimmed)
			.build();
	}

	/**
	 * 채팅방 참여자 중 발신자가 아닌 쪽을 알림 수신자로 결정한다.
	 * (creator ↔ member 1:1 구조 전제)
	 */
	private static Long resolveRecipientId(ChatRoom room, Long senderId) {
		return room.getCreatorId().equals(senderId) ? room.getMemberId() : room.getCreatorId();
	}

	/**
	 * 알림 미리보기 텍스트. TEXT 는 저장된 본문, IMAGE 는 {@link #IMAGE_PREVIEW} 고정 문구.
	 * 길이 제한/포맷은 알림 도메인 리스너 책임(현재는 원문 그대로 전달).
	 */
	private static String buildContentPreview(ChatMessage message) {
		if (message.getType() == ChatMessageType.IMAGE) {
			return IMAGE_PREVIEW;
		}
		return message.getContent();
	}

	private ChatMessage buildImageMessage(Long chatRoomId, Long senderId, MultipartFile image) {
		if (image == null || image.isEmpty()) {
			throw new CustomException(ChatErrorCode.CHAT_MESSAGE_INVALID);
		}
		StoredObject stored = objectStorage.upload("chat/" + chatRoomId, image);
		return ChatMessage.builder()
			.chatRoomId(chatRoomId)
			.senderId(senderId)
			.type(ChatMessageType.IMAGE)
			.imageUrl(stored.url())
			.imageStorageKey(stored.storageKey())
			.imageOriginalName(stored.originalName())
			.build();
	}
}
