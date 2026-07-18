package com.white.handdam.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.board.service.PaidSubscriptionChecker;
import com.white.handdam.chat.dto.response.ChatMessageResponse;
import com.white.handdam.chat.dto.response.ChatReadResponse;
import com.white.handdam.chat.entity.ChatMessage;
import com.white.handdam.chat.entity.ChatMessageType;
import com.white.handdam.chat.entity.ChatRoom;
import com.white.handdam.chat.entity.ChatRoomStatus;
import com.white.handdam.chat.event.ChatMessageSentEvent;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.repository.ChatMessageRepository;
import com.white.handdam.chat.websocket.publisher.ChatMessagePublisher;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.ErrorCode;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

	@Mock
	private ChatRoomService chatRoomService;

	@Mock
	private ChatMessageRepository chatMessageRepository;

	@Mock
	private PaidSubscriptionChecker paidSubscriptionChecker;

	@Mock
	private ObjectStorage objectStorage;

	@Mock
	private ChatMessagePublisher chatMessagePublisher;

	@Mock
	private ApplicationEventPublisher eventPublisher;

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

	@Test
	@DisplayName("활성 유료 구독 멤버는 텍스트 메시지를 전송할 수 있다")
	void paidMemberCanSendTextMessage() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = activeRoom(creatorId, memberId);
		given(chatRoomService.requireParticipatingRoom(10L, memberId)).willReturn(room);
		given(paidSubscriptionChecker.hasActivePaidSubscription(memberId, creatorId)).willReturn(true);
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
			ChatMessage msg = invocation.getArgument(0);
			ReflectionTestUtils.setField(msg, "id", 200L);
			ReflectionTestUtils.setField(msg, "sentAt", Instant.parse("2026-07-17T01:00:00Z"));
			return msg;
		});

		ChatMessageResponse response = chatMessageService.sendMessage(
			10L, memberId, ChatMessageType.TEXT, "안녕하세요", null
		);

		assertThat(response.id()).isEqualTo(200L);
		assertThat(response.type()).isEqualTo(ChatMessageType.TEXT);
		assertThat(response.content()).isEqualTo("안녕하세요");
		assertThat(room.getLastMessageAt()).isEqualTo(Instant.parse("2026-07-17T01:00:00Z"));
		verify(objectStorage, never()).upload(any(), any());

		ArgumentCaptor<ChatMessageSentEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		ChatMessageSentEvent event = eventCaptor.getValue();
		assertThat(event.chatRoomId()).isEqualTo(10L);
		assertThat(event.messageId()).isEqualTo(200L);
		assertThat(event.senderId()).isEqualTo(memberId);
		assertThat(event.recipientId()).isEqualTo(creatorId);
		assertThat(event.type()).isEqualTo(ChatMessageType.TEXT);
		assertThat(event.contentPreview()).isEqualTo("안녕하세요");
	}

	@Test
	@DisplayName("크리에이터(작성자)는 구독 검사 없이 메시지를 전송할 수 있다")
	void creatorCanSendWithoutSubscriptionCheck() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = activeRoom(creatorId, memberId);
		given(chatRoomService.requireParticipatingRoom(10L, creatorId)).willReturn(room);
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
			ChatMessage msg = invocation.getArgument(0);
			ReflectionTestUtils.setField(msg, "id", 201L);
			ReflectionTestUtils.setField(msg, "sentAt", Instant.parse("2026-07-17T02:00:00Z"));
			return msg;
		});

		ChatMessageResponse response = chatMessageService.sendMessage(
			10L, creatorId, ChatMessageType.TEXT, "답장입니다", null
		);

		assertThat(response.senderId()).isEqualTo(creatorId);
		verify(paidSubscriptionChecker, never()).hasActivePaidSubscription(any(), any());
	}

	@Test
	@DisplayName("이미지 메시지는 S3 업로드 후 저장된다")
	void canSendImageMessage() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = activeRoom(creatorId, memberId);
		MultipartFile image = new MockMultipartFile(
			"image", "photo.jpg", "image/jpeg", new byte[] {1, 2, 3}
		);
		given(chatRoomService.requireParticipatingRoom(10L, memberId)).willReturn(room);
		given(paidSubscriptionChecker.hasActivePaidSubscription(memberId, creatorId)).willReturn(true);
		given(objectStorage.upload(eq("chat/10"), eq(image)))
			.willReturn(new StoredObject("chat/10/key.jpg", "https://cdn/photo.jpg", "photo.jpg"));
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
			ChatMessage msg = invocation.getArgument(0);
			ReflectionTestUtils.setField(msg, "id", 202L);
			ReflectionTestUtils.setField(msg, "sentAt", Instant.parse("2026-07-17T03:00:00Z"));
			return msg;
		});

		ChatMessageResponse response = chatMessageService.sendMessage(
			10L, memberId, ChatMessageType.IMAGE, null, image
		);

		assertThat(response.type()).isEqualTo(ChatMessageType.IMAGE);
		assertThat(response.imageUrl()).isEqualTo("https://cdn/photo.jpg");
		assertThat(response.imageOriginalName()).isEqualTo("photo.jpg");

		ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
		verify(chatMessageRepository).save(captor.capture());
		assertThat(captor.getValue().getImageStorageKey()).isEqualTo("chat/10/key.jpg");

		ArgumentCaptor<ChatMessageSentEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		ChatMessageSentEvent event = eventCaptor.getValue();
		assertThat(event.chatRoomId()).isEqualTo(10L);
		assertThat(event.messageId()).isEqualTo(202L);
		assertThat(event.type()).isEqualTo(ChatMessageType.IMAGE);
		assertThat(event.senderId()).isEqualTo(memberId);
		assertThat(event.recipientId()).isEqualTo(creatorId);
		assertThat(event.contentPreview()).isEqualTo("[이미지]");
	}

	@Test
	@DisplayName("종료된 채팅방에서는 메시지를 전송할 수 없다")
	void cannotSendInClosedRoom() {
		ChatRoom room = activeRoom(1L, 99L);
		ReflectionTestUtils.setField(room, "status", ChatRoomStatus.CLOSED);
		given(chatRoomService.requireParticipatingRoom(10L, 99L)).willReturn(room);

		assertThatThrownBy(() -> chatMessageService.sendMessage(
			10L, 99L, ChatMessageType.TEXT, "hi", null
		)).satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_ROOM_CLOSED));

		verify(chatMessageRepository, never()).save(any());
	}

	@Test
	@DisplayName("구독이 만료된 멤버는 메시지를 전송할 수 없다")
	void expiredSubscriberCannotSend() {
		ChatRoom room = activeRoom(1L, 99L);
		given(chatRoomService.requireParticipatingRoom(10L, 99L)).willReturn(room);
		given(paidSubscriptionChecker.hasActivePaidSubscription(99L, 1L)).willReturn(false);

		assertThatThrownBy(() -> chatMessageService.sendMessage(
			10L, 99L, ChatMessageType.TEXT, "hi", null
		)).satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_MESSAGE_SEND_SUBSCRIPTION_REQUIRED));

		verify(chatMessageRepository, never()).save(any());
	}

	@Test
	@DisplayName("텍스트 내용이 비어 있으면 전송할 수 없다")
	void blankTextIsInvalid() {
		ChatRoom room = activeRoom(1L, 99L);
		given(chatRoomService.requireParticipatingRoom(10L, 99L)).willReturn(room);
		given(paidSubscriptionChecker.hasActivePaidSubscription(99L, 1L)).willReturn(true);

		assertThatThrownBy(() -> chatMessageService.sendMessage(
			10L, 99L, ChatMessageType.TEXT, "  ", null
		)).satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_MESSAGE_INVALID));
	}

	@Test
	@DisplayName("이미지 파일이 없으면 이미지 메시지를 전송할 수 없다")
	void missingImageIsInvalid() {
		ChatRoom room = activeRoom(1L, 99L);
		given(chatRoomService.requireParticipatingRoom(10L, 1L)).willReturn(room);

		assertThatThrownBy(() -> chatMessageService.sendMessage(
			10L, 1L, ChatMessageType.IMAGE, null, null
		)).satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_MESSAGE_INVALID));

		verify(objectStorage, never()).upload(any(), any());
	}

	@Test
	@DisplayName("참여자가 아니면 메시지를 전송할 수 없다")
	void nonParticipantCannotSend() {
		given(chatRoomService.requireParticipatingRoom(10L, 7L))
			.willThrow(new CustomException(ChatErrorCode.CHAT_NOT_PARTICIPANT));

		assertThatThrownBy(() -> chatMessageService.sendMessage(
			10L, 7L, ChatMessageType.TEXT, "hi", null
		)).satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_NOT_PARTICIPANT));
	}

	@Test
	@DisplayName("멤버는 상대방 미확인 메시지를 일괄 읽음 처리할 수 있다")
	void memberCanMarkOpponentMessagesAsRead() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = activeRoom(creatorId, memberId);
		given(chatRoomService.requireParticipatingRoom(10L, memberId)).willReturn(room);
		given(chatMessageRepository.markOpponentMessagesAsRead(eq(10L), eq(memberId), any(Instant.class)))
			.willReturn(3);

		ChatReadResponse response = chatMessageService.markMessagesAsRead(10L, memberId);

		assertThat(response.updatedCount()).isEqualTo(3L);
		verify(paidSubscriptionChecker, never()).hasActivePaidSubscription(any(), any());
	}

	@Test
	@DisplayName("크리에이터(작성자)도 상대 메시지를 일괄 읽음 처리할 수 있다")
	void creatorCanMarkOpponentMessagesAsRead() {
		Long creatorId = 1L;
		Long memberId = 99L;
		ChatRoom room = activeRoom(creatorId, memberId);
		given(chatRoomService.requireParticipatingRoom(10L, creatorId)).willReturn(room);
		given(chatMessageRepository.markOpponentMessagesAsRead(eq(10L), eq(creatorId), any(Instant.class)))
			.willReturn(1);

		assertThat(chatMessageService.markMessagesAsRead(10L, creatorId).updatedCount()).isEqualTo(1L);
	}

	@Test
	@DisplayName("종료된 채팅방에서도 참여자는 읽음 처리할 수 있다")
	void participantCanMarkReadInClosedRoom() {
		ChatRoom room = activeRoom(1L, 99L);
		ReflectionTestUtils.setField(room, "status", ChatRoomStatus.CLOSED);
		given(chatRoomService.requireParticipatingRoom(10L, 99L)).willReturn(room);
		given(chatMessageRepository.markOpponentMessagesAsRead(eq(10L), eq(99L), any(Instant.class)))
			.willReturn(2);

		assertThat(chatMessageService.markMessagesAsRead(10L, 99L).updatedCount()).isEqualTo(2L);
	}

	@Test
	@DisplayName("참여자가 아니면 읽음 처리할 수 없다")
	void nonParticipantCannotMarkAsRead() {
		given(chatRoomService.requireParticipatingRoom(10L, 7L))
			.willThrow(new CustomException(ChatErrorCode.CHAT_NOT_PARTICIPANT));

		assertThatThrownBy(() -> chatMessageService.markMessagesAsRead(10L, 7L))
			.satisfies(ex -> assertErrorCode(ex, ChatErrorCode.CHAT_NOT_PARTICIPANT));

		verify(chatMessageRepository, never()).markOpponentMessagesAsRead(any(), any(), any());
	}

	private static ChatRoom activeRoom(Long creatorId, Long memberId) {
		ChatRoom room = ChatRoom.builder().creatorId(creatorId).memberId(memberId).build();
		ReflectionTestUtils.setField(room, "id", 10L);
		return room;
	}

	private static void assertErrorCode(Throwable thrown, ErrorCode expected) {
		assertThat(thrown).isInstanceOf(CustomException.class);
		assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(expected);
	}
}
