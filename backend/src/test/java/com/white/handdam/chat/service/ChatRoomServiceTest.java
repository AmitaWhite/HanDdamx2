package com.white.handdam.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.board.service.PaidSubscriptionChecker;
import com.white.handdam.chat.dto.response.ChatRoomResponse;
import com.white.handdam.chat.entity.ChatRoom;
import com.white.handdam.chat.entity.ChatRoomStatus;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.repository.ChatRoomRepository;
import com.white.handdam.chat.service.ChatRoomService.CreateOrGetResult;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

	@Mock
	private ChatRoomRepository chatRoomRepository;

	@Mock
	private PaidSubscriptionChecker paidSubscriptionChecker;

	@InjectMocks
	private ChatRoomService chatRoomService;

	@Test
	@DisplayName("활성 유료 구독자는 새 채팅방을 생성할 수 있다")
	void paidSubscriberCanCreateChatRoom() {
		Long creatorId = 1L;
		Long memberId = 99L;
		given(paidSubscriptionChecker.hasActivePaidSubscription(memberId, creatorId)).willReturn(true);
		given(chatRoomRepository.findByCreatorIdAndMemberId(creatorId, memberId))
			.willReturn(Optional.empty());
		given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
			ChatRoom room = invocation.getArgument(0);
			ReflectionTestUtils.setField(room, "id", 10L);
			ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-07-16T00:00:00Z"));
			ReflectionTestUtils.setField(room, "updatedAt", Instant.parse("2026-07-16T00:00:00Z"));
			return room;
		});

		CreateOrGetResult result = chatRoomService.createOrGetChatRoom(creatorId, memberId);

		assertThat(result.created()).isTrue();
		ChatRoomResponse room = result.room();
		assertThat(room.id()).isEqualTo(10L);
		assertThat(room.creatorId()).isEqualTo(creatorId);
		assertThat(room.memberId()).isEqualTo(memberId);
		assertThat(room.status()).isEqualTo(ChatRoomStatus.ACTIVE);
		verify(chatRoomRepository).save(any(ChatRoom.class));
	}

	@Test
	@DisplayName("이미 채팅방이 있으면 기존 방을 반환하고 새로 만들지 않는다")
	void returnsExistingChatRoom() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom existing = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(existing, "id", 7L);
		ReflectionTestUtils.setField(existing, "createdAt", Instant.parse("2026-07-16T00:00:00Z"));
		ReflectionTestUtils.setField(existing, "updatedAt", Instant.parse("2026-07-16T00:00:00Z"));

		given(paidSubscriptionChecker.hasActivePaidSubscription(memberId, creatorId)).willReturn(true);
		given(chatRoomRepository.findByCreatorIdAndMemberId(creatorId, memberId))
			.willReturn(Optional.of(existing));

		CreateOrGetResult result = chatRoomService.createOrGetChatRoom(creatorId, memberId);

		assertThat(result.created()).isFalse();
		assertThat(result.room().id()).isEqualTo(7L);
		verify(chatRoomRepository, never()).save(any());
	}

	@Test
	@DisplayName("유료 구독이 없으면 채팅방을 생성할 수 없다")
	void deniedWithoutPaidSubscription() {
		Long creatorId = 1L;
		Long memberId = 7L;
		given(paidSubscriptionChecker.hasActivePaidSubscription(memberId, creatorId)).willReturn(false);

		assertThatThrownBy(() -> chatRoomService.createOrGetChatRoom(creatorId, memberId))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_SUBSCRIPTION_REQUIRED));

		verify(chatRoomRepository, never()).findByCreatorIdAndMemberId(any(), any());
		verify(chatRoomRepository, never()).save(any());
	}

	@Test
	@DisplayName("자기 자신과는 채팅할 수 없다")
	void cannotChatWithSelf() {
		assertThatThrownBy(() -> chatRoomService.createOrGetChatRoom(1L, 1L))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_SELF_NOT_ALLOWED));

		verify(chatRoomRepository, never()).save(any());
	}

	@Test
	@DisplayName("로그인이 없으면 채팅방을 생성할 수 없다")
	void loginRequired() {
		assertThatThrownBy(() -> chatRoomService.createOrGetChatRoom(1L, null))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_LOGIN_REQUIRED));
	}

	private static void assertErrorCode(Throwable thrown, ErrorCode expected) {
		assertThat(thrown).isInstanceOf(CustomException.class);
		assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(expected);
	}
}
