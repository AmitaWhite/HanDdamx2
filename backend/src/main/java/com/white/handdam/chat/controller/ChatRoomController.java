package com.white.handdam.chat.controller;

import com.white.handdam.chat.dto.response.ChatRoomResponse;
import com.white.handdam.chat.service.ChatRoomService;
import com.white.handdam.chat.service.ChatRoomService.CreateOrGetResult;
import com.white.handdam.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

	private final ChatRoomService chatRoomService;

	/**
	 * 크리에이터와 1:1 채팅방 생성 또는 기존 방 반환 (CHAT-001).
	 * 권한: 활성 유료 구독자
	 */
	@PostMapping("/api/creators/{creatorId}/chat-rooms")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetChatRoom(
		@PathVariable Long creatorId,
		@RequestHeader("X-Member-Id") Long memberId
	) {
		CreateOrGetResult result = chatRoomService.createOrGetChatRoom(creatorId, memberId);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(ApiResponse.success(result.room()));
	}
}
