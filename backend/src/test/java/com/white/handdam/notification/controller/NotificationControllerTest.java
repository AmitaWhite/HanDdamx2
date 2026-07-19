package com.white.handdam.notification.controller;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.GlobalExceptionHandler;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.member.entity.Role;
import com.white.handdam.notification.dto.response.NotificationReadAllResponse;
import com.white.handdam.notification.dto.response.NotificationResponse;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.exception.NotificationErrorCode;
import com.white.handdam.notification.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class NotificationControllerTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long SENDER_ID = 2L;
	private static final Long CHAT_ROOM_ID = 42L;
	private static final Long NOTIFICATION_ID = 100L;
	private static final Instant CREATED_AT = Instant.parse("2026-07-20T00:00:00Z");

	private NotificationService notificationService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		notificationService = Mockito.mock(NotificationService.class);
		mockMvc = standaloneSetup(new NotificationController(notificationService))
			// standalone 에는 Spring Data 웹 설정이 없어 Pageable 리졸버를 직접 등록해야 한다
			// (실제 앱에서는 자동 구성으로 등록된다)
			.setCustomArgumentResolvers(
				new AuthenticationPrincipalArgumentResolver(),
				new PageableHandlerMethodArgumentResolver()
			)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("GET /api/notifications - 200과 SliceResponse 형태로 알림 목록을 반환한다")
	void getMyNotifications() throws Exception {
		authenticate();
		Pageable pageable = PageRequest.of(0, 20);
		when(notificationService.getMyNotifications(eq(MEMBER_ID), any(Pageable.class)))
			.thenReturn(new SliceImpl<>(List.of(sampleResponse()), pageable, false));

		mockMvc.perform(get("/api/notifications"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.content", hasSize(1)))
			.andExpect(jsonPath("$.data.content[0].id").value(NOTIFICATION_ID))
			.andExpect(jsonPath("$.data.content[0].type").value("CHAT_MESSAGE"))
			.andExpect(jsonPath("$.data.content[0].referenceType").value("CHAT_ROOM"))
			.andExpect(jsonPath("$.data.hasNext").value(false))
			.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("GET /api/notifications/unread-count - 안 읽은 알림 개수를 반환한다")
	void getUnreadCount() throws Exception {
		authenticate();
		when(notificationService.countUnread(MEMBER_ID)).thenReturn(5L);

		mockMvc.perform(get("/api/notifications/unread-count"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.unreadCount").value(5));
	}

	@Test
	@DisplayName("PATCH /api/notifications/{id}/read - 200과 빈 데이터를 반환한다")
	void markAsRead() throws Exception {
		authenticate();

		mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		verify(notificationService).markAsRead(NOTIFICATION_ID, MEMBER_ID);
	}

	@Test
	@DisplayName("PATCH /api/notifications/{id}/read - 본인 알림이 아니면 403을 반환한다")
	void markAsRead_notOwner() throws Exception {
		authenticate();
		doThrow(new CustomException(NotificationErrorCode.NOTIFICATION_FORBIDDEN))
			.when(notificationService).markAsRead(NOTIFICATION_ID, MEMBER_ID);

		mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("NOTIFICATION_FORBIDDEN"));
	}

	@Test
	@DisplayName("PATCH /api/notifications/{id}/read - 알림이 없으면 404를 반환한다")
	void markAsRead_notFound() throws Exception {
		authenticate();
		doThrow(new CustomException(NotificationErrorCode.NOTIFICATION_NOT_FOUND))
			.when(notificationService).markAsRead(NOTIFICATION_ID, MEMBER_ID);

		mockMvc.perform(patch("/api/notifications/{notificationId}/read", NOTIFICATION_ID))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.code").value("NOTIFICATION_NOT_FOUND"));
	}

	@Test
	@DisplayName("PATCH /api/notifications/read-all - 갱신 건수를 반환한다")
	void markAllAsRead() throws Exception {
		authenticate();
		when(notificationService.markAllAsRead(MEMBER_ID))
			.thenReturn(new NotificationReadAllResponse(3L));

		mockMvc.perform(patch("/api/notifications/read-all"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.updatedCount").value(3));

		verify(notificationService).markAllAsRead(MEMBER_ID);
	}

	private void authenticate() {
		AuthMember authMember = new AuthMember(MEMBER_ID, Role.USER);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(authMember, null)
		);
	}

	private NotificationResponse sampleResponse() {
		return new NotificationResponse(
			NOTIFICATION_ID,
			SENDER_ID,
			NotificationType.CHAT_MESSAGE,
			"안녕하세요",
			CHAT_ROOM_ID,
			"CHAT_ROOM",
			false,
			CREATED_AT
		);
	}
}
