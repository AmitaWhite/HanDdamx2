package com.white.handdam.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.entity.ChatMessage;
import com.white.handdam.chat.entity.ChatMessageType;
import com.white.handdam.chat.entity.ChatRoom;
import com.white.handdam.chat.entity.ChatRoomStatus;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.repository.ChatMessageRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

	@Mock
	private ChatRoomService chatRoomService;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@InjectMocks
	private ChatMessageService chatMessageService;

	private final Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "sentAt"));

	@Test
	@DisplayName("멤버(참여자)는 메시지 목록을 조회할 수 있다")
	void memberCanGetMessages() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);

		ChatMessage message = ChatMessage.builder()
			.chatRoomId(10L)
			.senderId(creatorId)
			.type(ChatMessageType.TEXT)
			.content("안녕")
			.build();
		ReflectionTestUtils.setField(message, "id", 100L);
		ReflectionTestUtils.setField(message, "sentAt", Instant.parse("2026-07-16T12:00:00Z"));

		given(chatRoomService.requireParticipatingRoom(10L, memberId)).willReturn(room);
		given(chatMessageRepository.findByChatRoomId(eq(10L), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(message), pageable, 1));

		Page<ChatMessageResponse> result = chatMessageService.getMessages(10L, memberId, pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).content()).isEqualTo("안녕");
		assertThat(result.getContent().get(0).senderId()).isEqualTo(creatorId);
	}

	@Test
	@DisplayName("크리에이터(작성자)는 메시지 목록을 조회할 수 있다")
	void creatorCanGetMessages() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		given(chatRoomService.requireParticipatingRoom(10L, creatorId)).willReturn(room);
		given(chatMessageRepository.findByChatRoomId(eq(10L), eq(pageable)))
			.willReturn(Page.empty(pageable));

		Page<ChatMessageResponse> result = chatMessageService.getMessages(10L, creatorId, pageable);

		assertThat(result.getContent()).isEmpty();
		verify(chatMessageRepository).findByChatRoomId(10L, pageable);
	}

	@Test
	@DisplayName("종료된 채팅방도 참여자는 메시지 목록을 조회할 수 있다")
	void participantCanGetMessagesFromClosedRoom() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		ReflectionTestUtils.setField(room, "status", ChatRoomStatus.CLOSED);
		given(chatRoomService.requireParticipatingRoom(10L, memberId)).willReturn(room);
		given(chatMessageRepository.findByChatRoomId(eq(10L), eq(pageable)))
			.willReturn(Page.empty(pageable));

		assertThat(chatMessageService.getMessages(10L, memberId, pageable).getContent()).isEmpty();
	}

	@Test
	@DisplayName("참여자가 아니면 메시지 목록을 조회할 수 없다")
	void nonParticipantCannotGetMessages() {
		given(chatRoomService.requireParticipatingRoom(10L, 7L))
			.willThrow(new CustomException(ChatErrorCode.CHAT_NOT_PARTICIPANT));

		assertThatThrownBy(() -> chatMessageService.getMessages(10L, 7L, pageable))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_NOT_PARTICIPANT));

		verify(chatMessageRepository, never()).findByChatRoomId(any(), any());
	}

	@Test
	@DisplayName("없는 채팅방은 NOT_FOUND를 반환한다")
	void chatRoomNotFound() {
		given(chatRoomService.requireParticipatingRoom(999L, 1L))
			.willThrow(new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

		assertThatThrownBy(() -> chatMessageService.getMessages(999L, 1L, pageable))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	@Test
	@DisplayName("로그인이 없으면 메시지 목록을 조회할 수 없다")
	void loginRequired() {
		given(chatRoomService.requireParticipatingRoom(10L, null))
			.willThrow(new CustomException(ChatErrorCode.CHAT_LOGIN_REQUIRED));

		assertThatThrownBy(() -> chatMessageService.getMessages(10L, null, pageable))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_LOGIN_REQUIRED));
	}

	private static void assertErrorCode(Throwable thrown, ErrorCode expected) {
		assertThat(thrown).isInstanceOf(CustomException.class);
		assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(expected);
	}
}
