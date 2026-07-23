package com.white.handdam.chat.controller;

import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.entity.ChatMessageType;
import com.white.handdam.chat.service.ChatMessageService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {

	private final ChatMessageService chatMessageService;

	/**
	 * 채팅 메시지 내역 조회 (CHAT-006).
	 * 권한: 해당 채팅방 참여자(creator 또는 member)
	 */
	@GetMapping("/api/chat-rooms/{chatRoomId}/messages")
	public ApiResponse<Page<ChatMessageResponse>> getMessages(
		@PathVariable Long chatRoomId,
		@AuthenticationPrincipal AuthMember member,
		@PageableDefault(size = 20, sort = "sentAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.success(chatMessageService.getMessages(chatRoomId, member.id(), pageable));
	}

	/**
	 * 텍스트·이미지 메시지 전송 (CHAT-002~004).
	 * 권한: ACTIVE 방 참여자 — 구독자는 활성 유료 구독, 크리에이터는 작성자로 전송 가능
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(
		value = "/api/chat-rooms/{chatRoomId}/messages",
		consumes = MediaType.MULTIPART_FORM_DATA_VALUE
	)
	public ApiResponse<ChatMessageResponse> sendMessage(
		@PathVariable Long chatRoomId,
		@AuthenticationPrincipal AuthMember member,
		@RequestParam ChatMessageType type,
		@RequestParam(required = false) String content,
		@RequestPart(value = "image", required = false) MultipartFile image
	) {
		return ApiResponse.success(
			chatMessageService.sendMessage(chatRoomId, member.id(), type, content, image)
		);
	}
}
