package com.white.handdam.chat.controller;

import com.white.handdam.chat.dto.response.ChatReadResponse;
import com.white.handdam.chat.dto.response.ChatRoomListItemResponse;
import com.white.handdam.chat.dto.response.ChatRoomResponse;
import com.white.handdam.chat.service.ChatMessageService;
import com.white.handdam.chat.service.ChatRoomService;
import com.white.handdam.chat.service.ChatRoomService.CreateOrGetResult;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

	private final ChatRoomService chatRoomService;
	private final ChatMessageService chatMessageService;

	/**
	 * 크리에이터와 1:1 채팅방 생성 또는 기존 방 반환 (CHAT-001).
	 * 권한: 활성 유료 구독자 (JWT 인증 필요)
	 */
	@PostMapping("/api/creators/{creatorId}/chat-rooms")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> createOrGetChatRoom(
		@PathVariable Long creatorId,
		@AuthenticationPrincipal AuthMember member
	) {
		CreateOrGetResult result = chatRoomService.createOrGetChatRoom(creatorId, member.id());
		//새로만든방이면 201 반환, 기존방이면 200 반환
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(ApiResponse.success(result.room()));
	}

	/**
	 * 참여 채팅방 목록 + 마지막 메시지 조회 (CHAT-005).
	 * 권한: 로그인 사용자(본인이 creator 또는 member인 방만)
	 */
	@GetMapping("/api/chat-rooms")
	public ResponseEntity<ApiResponse<List<ChatRoomListItemResponse>>> getMyChatRooms(
		@AuthenticationPrincipal AuthMember member
	) {
		return ResponseEntity.ok(ApiResponse.success(chatRoomService.getMyChatRooms(member.id())));
	}

	/**
	 * 채팅방 상세·상태 조회 (CHAT-009).
	 * 권한: 해당 채팅방 참여자(creator 또는 member)
	 */
	@GetMapping("/api/chat-rooms/{chatRoomId}")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> getChatRoom(
		@PathVariable Long chatRoomId,
		@AuthenticationPrincipal AuthMember member
	) {
		return ResponseEntity.ok(ApiResponse.success(chatRoomService.getChatRoom(chatRoomId, member.id())));
	}

	/**
	 * 상대방 미확인 메시지 일괄 읽음 (CHAT-007).
	 * 권한: 해당 채팅방 참여자(creator 또는 member)
	 */
	@PatchMapping("/api/chat-rooms/{chatRoomId}/read")
	public ApiResponse<ChatReadResponse> markMessagesAsRead(
		@PathVariable Long chatRoomId,
		@AuthenticationPrincipal AuthMember member
	) {
		return ApiResponse.success(chatMessageService.markMessagesAsRead(chatRoomId, member.id()));
	}
}
