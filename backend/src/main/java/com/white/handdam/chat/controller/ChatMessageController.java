package com.white.handdam.chat.controller;

import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.service.ChatMessageService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {

	private final ChatMessageService chatMessageService;

	/**
	 * 채팅 메시지 내역 조회 (CHAT-006).
	 * 권한: 해당 채팅방 참여자(creator 또는 member)
	 */
	@GetMapping("/api/chat-rooms/{chatRoomId}/messages")
	public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessages(
		@PathVariable Long chatRoomId,
		@AuthenticationPrincipal AuthMember member,
		@PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ResponseEntity.ok(
			ApiResponse.success(chatMessageService.getMessages(chatRoomId, member.id(), pageable))
		);
	}
}
