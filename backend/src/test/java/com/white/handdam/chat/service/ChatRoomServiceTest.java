package com.white.handdam.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.board.service.PaidSubscriptionChecker;
import com.white.handdam.chat.dto.response.ChatRoomListItemResponse;
import com.white.handdam.chat.dto.response.ChatRoomResponse;
import com.white.handdam.chat.entity.ChatMessage;
import com.white.handdam.chat.entity.ChatMessageType;
import com.white.handdam.chat.entity.ChatRoom;
import com.white.handdam.chat.entity.ChatRoomStatus;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.repository.ChatMessageRepository;
import com.white.handdam.chat.repository.ChatRoomRepository;
import com.white.handdam.chat.service.ChatRoomService.CreateOrGetResult;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
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
	private ChatMessageRepository chatMessageRepository;

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

	@Test
	@DisplayName("참여 채팅방 목록에 마지막 메시지와 미읽음 수를 포함한다")
	void listsParticipatingRoomsWithLastMessageAndUnread() {
		Long memberId = 2L;
		ChatRoom roomAsMember = ChatRoom.builder().creatorId(1L).memberId(memberId).build();
		ReflectionTestUtils.setField(roomAsMember, "id", 10L);
		ReflectionTestUtils.setField(roomAsMember, "lastMessageAt", Instant.parse("2026-07-16T12:00:00Z"));
		ReflectionTestUtils.setField(roomAsMember, "createdAt", Instant.parse("2026-07-16T00:00:00Z"));
		ReflectionTestUtils.setField(roomAsMember, "updatedAt", Instant.parse("2026-07-16T12:00:00Z"));

		ChatRoom roomAsCreator = ChatRoom.builder().creatorId(memberId).memberId(99L).build();
		ReflectionTestUtils.setField(roomAsCreator, "id", 11L);
		ReflectionTestUtils.setField(roomAsCreator, "lastMessageAt", Instant.parse("2026-07-16T10:00:00Z"));
		ReflectionTestUtils.setField(roomAsCreator, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
		ReflectionTestUtils.setField(roomAsCreator, "updatedAt", Instant.parse("2026-07-16T10:00:00Z"));

		ChatMessage lastMessage = ChatMessage.builder()
			.chatRoomId(10L)
			.senderId(1L)
			.type(ChatMessageType.TEXT)
			.content("안녕하세요")
			.build();
		ReflectionTestUtils.setField(lastMessage, "id", 100L);
		ReflectionTestUtils.setField(lastMessage, "sentAt", Instant.parse("2026-07-16T12:00:00Z"));

		given(chatRoomRepository.findParticipatingOrderByLastMessageAtDesc(memberId))
			.willReturn(List.of(roomAsMember, roomAsCreator));
		given(chatMessageRepository.findLatestByChatRoomIdIn(List.of(10L, 11L)))
			.willReturn(List.of(lastMessage));
		given(chatMessageRepository.countUnreadByChatRoomIdIn(eq(List.of(10L, 11L)), eq(memberId)))
			.willReturn(List.<Object[]>of(new Object[] {10L, 3L}));

		List<ChatRoomListItemResponse> result = chatRoomService.getMyChatRooms(memberId);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).id()).isEqualTo(10L);
		assertThat(result.get(0).lastMessage().content()).isEqualTo("안녕하세요");
		assertThat(result.get(0).unreadCount()).isEqualTo(3L);
		assertThat(result.get(1).id()).isEqualTo(11L);
		assertThat(result.get(1).lastMessage()).isNull();
		assertThat(result.get(1).unreadCount()).isZero();
		verify(paidSubscriptionChecker, never()).hasActivePaidSubscription(any(), any());
	}

	@Test
	@DisplayName("참여 방이 없으면 빈 목록을 반환한다")
	void emptyChatRoomList() {
		given(chatRoomRepository.findParticipatingOrderByLastMessageAtDesc(5L)).willReturn(List.of());

		assertThat(chatRoomService.getMyChatRooms(5L)).isEmpty();
		verify(chatMessageRepository, never()).findLatestByChatRoomIdIn(any());
	}

	@Test
	@DisplayName("로그인이 없으면 채팅방 목록을 조회할 수 없다")
	void listLoginRequired() {
		assertThatThrownBy(() -> chatRoomService.getMyChatRooms(null))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_LOGIN_REQUIRED));
	}

	@Test
	@DisplayName("멤버(참여자)는 채팅방 상세를 조회할 수 있다")
	void memberCanGetChatRoomDetail() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-07-16T00:00:00Z"));
		ReflectionTestUtils.setField(room, "updatedAt", Instant.parse("2026-07-16T00:00:00Z"));
		given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));

		ChatRoomResponse response = chatRoomService.getChatRoom(10L, memberId);

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.status()).isEqualTo(ChatRoomStatus.ACTIVE);
		assertThat(response.creatorId()).isEqualTo(creatorId);
		assertThat(response.memberId()).isEqualTo(memberId);
		verify(paidSubscriptionChecker, never()).hasActivePaidSubscription(any(), any());
	}

	@Test
	@DisplayName("크리에이터(작성자)는 채팅방 상세를 조회할 수 있다")
	void creatorCanGetChatRoomDetail() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-07-16T00:00:00Z"));
		ReflectionTestUtils.setField(room, "updatedAt", Instant.parse("2026-07-16T00:00:00Z"));
		given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));

		ChatRoomResponse response = chatRoomService.getChatRoom(10L, creatorId);

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.creatorId()).isEqualTo(creatorId);
	}

	@Test
	@DisplayName("종료된 채팅방도 참여자는 상세를 조회할 수 있다")
	void participantCanGetClosedChatRoomDetail() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		ReflectionTestUtils.setField(room, "status", ChatRoomStatus.CLOSED);
		ReflectionTestUtils.setField(room, "closedBy", memberId);
		ReflectionTestUtils.setField(room, "closedAt", Instant.parse("2026-07-16T12:00:00Z"));
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-07-16T00:00:00Z"));
		ReflectionTestUtils.setField(room, "updatedAt", Instant.parse("2026-07-16T12:00:00Z"));
		given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));

		ChatRoomResponse response = chatRoomService.getChatRoom(10L, memberId);

		assertThat(response.status()).isEqualTo(ChatRoomStatus.CLOSED);
		assertThat(response.closedBy()).isEqualTo(memberId);
		assertThat(response.closedAt()).isEqualTo(Instant.parse("2026-07-16T12:00:00Z"));
	}

	@Test
	@DisplayName("참여자가 아니면 채팅방 상세를 조회할 수 없다")
	void nonParticipantCannotGetChatRoomDetail() {
		ChatRoom room = ChatRoom.builder().creatorId(1L).memberId(99L).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));

		assertThatThrownBy(() -> chatRoomService.getChatRoom(10L, 7L))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_NOT_PARTICIPANT));
	}

	@Test
	@DisplayName("없는 채팅방은 NOT_FOUND를 반환한다")
	void chatRoomNotFound() {
		given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> chatRoomService.getChatRoom(999L, 1L))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_ROOM_NOT_FOUND));
	}

	@Test
	@DisplayName("로그인이 없으면 채팅방 상세를 조회할 수 없다")
	void detailLoginRequired() {
		assertThatThrownBy(() -> chatRoomService.getChatRoom(10L, null))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_LOGIN_REQUIRED));
	}

	@Test
	@DisplayName("멤버(참여자)는 채팅방을 종료할 수 있다")
	void memberCanCloseChatRoom() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-07-16T00:00:00Z"));
		ReflectionTestUtils.setField(room, "updatedAt", Instant.parse("2026-07-16T00:00:00Z"));
		given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));

		ChatRoomResponse response = chatRoomService.closeChatRoom(10L, memberId);

		assertThat(response.status()).isEqualTo(ChatRoomStatus.CLOSED);
		assertThat(response.closedBy()).isEqualTo(memberId);
		assertThat(response.closedAt()).isNotNull();
		verify(paidSubscriptionChecker, never()).hasActivePaidSubscription(any(), any());
	}

	@Test
	@DisplayName("크리에이터(작성자)는 채팅방을 종료할 수 있다")
	void creatorCanCloseChatRoom() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		ReflectionTestUtils.setField(room, "createdAt", Instant.parse("2026-07-16T00:00:00Z"));
		ReflectionTestUtils.setField(room, "updatedAt", Instant.parse("2026-07-16T00:00:00Z"));
		given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));

		ChatRoomResponse response = chatRoomService.closeChatRoom(10L, creatorId);

		assertThat(response.status()).isEqualTo(ChatRoomStatus.CLOSED);
		assertThat(response.closedBy()).isEqualTo(creatorId);
	}

	@Test
	@DisplayName("이미 종료된 채팅방은 다시 종료할 수 없다")
	void cannotCloseAlreadyClosedRoom() {
		ChatRoom room = ChatRoom.builder().creatorId(1L).memberId(99L).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		ReflectionTestUtils.setField(room, "status", ChatRoomStatus.CLOSED);
		ReflectionTestUtils.setField(room, "closedBy", 99L);
		ReflectionTestUtils.setField(room, "closedAt", Instant.parse("2026-07-16T12:00:00Z"));
		given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));

		assertThatThrownBy(() -> chatRoomService.closeChatRoom(10L, 99L))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_ROOM_ALREADY_CLOSED));
	}

	@Test
	@DisplayName("참여자가 아니면 채팅방을 종료할 수 없다")
	void nonParticipantCannotClose() {
		ChatRoom room = ChatRoom.builder().creatorId(1L).memberId(99L).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		given(chatRoomRepository.findById(10L)).willReturn(Optional.of(room));

		assertThatThrownBy(() -> chatRoomService.closeChatRoom(10L, 7L))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_NOT_PARTICIPANT));
	}

	private static void assertErrorCode(Throwable thrown, ErrorCode expected) {
		assertThat(thrown).isInstanceOf(CustomException.class);
		assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(expected);
	}
}
