package com.white.handdam.chat.service;

import com.white.handdam.chat.converter.ChatMessageConverter;
import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

	private final ChatRoomService chatRoomService;
	private final ChatMessageRepository chatMessageRepository;

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
}
