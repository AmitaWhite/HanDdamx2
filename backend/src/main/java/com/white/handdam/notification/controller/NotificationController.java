package com.white.handdam.notification.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.response.SliceResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.notification.dto.response.NotificationResponse;
import com.white.handdam.notification.dto.response.UnreadCountResponse;
import com.white.handdam.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 조회·읽음 처리 API.
 *
 * 실시간 수신은 REST 가 아니라 STOMP 구독으로 처리한다 —
 * 클라이언트가 기존 {@code /ws} 로 연결한 뒤 {@code /sub/notifications/{본인id}} 를 구독하면 된다.
 * 따라서 구독용 엔드포인트는 없다.
 */
@RestController
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	/**
	 * 내 알림 목록 (최신순, 무한 스크롤).
	 * 권한: 로그인 사용자(본인 알림만)
	 */
	@GetMapping("/api/notifications")
	public ApiResponse<SliceResponse<NotificationResponse>> getMyNotifications(
			@AuthenticationPrincipal AuthMember member,
			Pageable pageable) {
		return ApiResponse.success(
				SliceResponse.from(notificationService.getMyNotifications(member.id(), pageable)));
	}

	/**
	 * 안 읽은 알림 개수 (뱃지).
	 * 권한: 로그인 사용자
	 */
	@GetMapping("/api/notifications/unread-count")
	public ApiResponse<UnreadCountResponse> getUnreadCount(
			@AuthenticationPrincipal AuthMember member) {
		return ApiResponse.success(
				new UnreadCountResponse(notificationService.countUnread(member.id())));
	}

	/**
	 * 알림 읽음 처리.
	 * 권한: 해당 알림의 수신자 본인
	 */
	@PatchMapping("/api/notifications/{notificationId}/read")
	public ApiResponse<Void> markAsRead(
			@PathVariable Long notificationId,
			@AuthenticationPrincipal AuthMember member) {
		notificationService.markAsRead(notificationId, member.id());
		return ApiResponse.noContent();
	}
}
