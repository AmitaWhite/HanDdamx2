package com.white.handdam.chat.websocket.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.white.handdam.chat.entity.ChatRoom;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.service.ChatRoomService;
import com.white.handdam.chat.websocket.publisher.ChatMessagePublisher;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.global.security.jwt.JwtTokenProvider;
import com.white.handdam.member.entity.Role;
import com.white.handdam.notification.exception.NotificationErrorCode;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private ChatRoomService chatRoomService;

	@Mock
	private MessageChannel messageChannel;

	@InjectMocks
	private StompAuthChannelInterceptor interceptor;

	@Test
	@DisplayName("채팅방 참여자는 채팅방 토픽을 구독할 수 있다")
	void participantCanSubscribeChatRoomTopic() {
		Long chatRoomId = 42L;
		Long memberId = 99L;
		given(chatRoomService.requireParticipatingRoom(chatRoomId, memberId))
			.willReturn(org.mockito.Mockito.mock(ChatRoom.class));

		Message<byte[]> message = subscribeMessage(
			ChatMessagePublisher.ROOM_TOPIC_PREFIX + chatRoomId,
			authMember(memberId)
		);

		assertThatCode(() -> interceptor.preSend(message, messageChannel))
			.doesNotThrowAnyException();
		verify(chatRoomService).requireParticipatingRoom(chatRoomId, memberId);
	}

	@Test
	@DisplayName("채팅방 비참여자는 채팅방 토픽 구독을 거부한다")
	void nonParticipantCannotSubscribeChatRoomTopic() {
		Long chatRoomId = 42L;
		Long outsiderId = 7L;
		willThrow(new CustomException(ChatErrorCode.CHAT_NOT_PARTICIPANT))
			.given(chatRoomService)
			.requireParticipatingRoom(chatRoomId, outsiderId);

		Message<byte[]> message = subscribeMessage(
			ChatMessagePublisher.ROOM_TOPIC_PREFIX + chatRoomId,
			authMember(outsiderId)
		);

		assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
			.satisfies(ex -> {
				assertThat(ex).isInstanceOf(CustomException.class);
				assertThat(((CustomException) ex).getErrorCode())
					.isEqualTo(ChatErrorCode.CHAT_NOT_PARTICIPANT);
			});
		verify(chatRoomService).requireParticipatingRoom(chatRoomId, outsiderId);
	}

	@Test
	@DisplayName("로그인하지 않은 사용자는 채팅방 토픽 구독을 거부한다")
	void unauthenticatedUserCannotSubscribeChatRoomTopic() {
		Message<byte[]> message = subscribeMessage(
			ChatMessagePublisher.ROOM_TOPIC_PREFIX + 42L,
			null
		);

		assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
			.satisfies(ex -> {
				assertThat(ex).isInstanceOf(CustomException.class);
				assertThat(((CustomException) ex).getErrorCode())
					.isEqualTo(ChatErrorCode.CHAT_LOGIN_REQUIRED);
			});
		verify(chatRoomService, never()).requireParticipatingRoom(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	@DisplayName("에러 큐 구독은 참여자 검사를 하지 않는다")
	void errorQueueSubscriptionDoesNotRequireParticipation() {
		Message<byte[]> message = subscribeMessage("/user/queue/errors", null);

		assertThatCode(() -> interceptor.preSend(message, messageChannel))
			.doesNotThrowAnyException();
		verify(chatRoomService, never()).requireParticipatingRoom(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	@DisplayName("잘못된 채팅방 destination은 구독을 거부한다")
	void invalidChatRoomDestinationIsRejected() {
		Message<byte[]> message = subscribeMessage(
			ChatMessagePublisher.ROOM_TOPIC_PREFIX + "abc",
			authMember(99L)
		);

		assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
			.satisfies(ex -> {
				assertThat(ex).isInstanceOf(CustomException.class);
				assertThat(((CustomException) ex).getErrorCode())
					.isEqualTo(ChatErrorCode.CHAT_ROOM_NOT_FOUND);
			});
		verify(chatRoomService, never()).requireParticipatingRoom(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	@DisplayName("본인 알림 토픽은 구독할 수 있다")
	void memberCanSubscribeOwnNotificationTopic() {
		Long memberId = 99L;
		Message<byte[]> message = subscribeMessage(
			NotificationPublisher.NOTIFICATION_TOPIC_PREFIX + memberId,
			authMember(memberId)
		);

		assertThatCode(() -> interceptor.preSend(message, messageChannel))
			.doesNotThrowAnyException();
		// 알림 토픽은 채팅방 참여자 검사를 타지 않는다 (두 prefix가 서로 배타적)
		verify(chatRoomService, never()).requireParticipatingRoom(
			org.mockito.ArgumentMatchers.any(),
			org.mockito.ArgumentMatchers.any()
		);
	}

	@Test
	@DisplayName("타인의 알림 토픽 구독은 거부한다")
	void memberCannotSubscribeOthersNotificationTopic() {
		Message<byte[]> message = subscribeMessage(
			NotificationPublisher.NOTIFICATION_TOPIC_PREFIX + 7L,
			authMember(99L)
		);

		assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
			.satisfies(ex -> {
				assertThat(ex).isInstanceOf(CustomException.class);
				assertThat(((CustomException) ex).getErrorCode())
					.isEqualTo(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
			});
	}

	@Test
	@DisplayName("로그인하지 않은 사용자는 알림 토픽 구독을 거부한다")
	void unauthenticatedUserCannotSubscribeNotificationTopic() {
		Message<byte[]> message = subscribeMessage(
			NotificationPublisher.NOTIFICATION_TOPIC_PREFIX + 99L,
			null
		);

		assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
			.satisfies(ex -> {
				assertThat(ex).isInstanceOf(CustomException.class);
				assertThat(((CustomException) ex).getErrorCode())
					.isEqualTo(ChatErrorCode.CHAT_LOGIN_REQUIRED);
			});
	}

	/**
	 * destination 파싱이 인증 검사보다 먼저 실행되므로,
	 * 형식이 잘못되면 로그인 여부와 무관하게 NOTIFICATION_FORBIDDEN 이 난다.
	 */
	@ParameterizedTest(name = "잘못된 알림 destination: \"{0}\"")
	@ValueSource(strings = {
		"/sub/notifications/",
		"/sub/notifications/1/2",
		"/sub/notifications/abc"
	})
	@DisplayName("잘못된 알림 destination은 구독을 거부한다")
	void invalidNotificationDestinationIsRejected(String destination) {
		Message<byte[]> message = subscribeMessage(destination, authMember(99L));

		assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
			.satisfies(ex -> {
				assertThat(ex).isInstanceOf(CustomException.class);
				assertThat(((CustomException) ex).getErrorCode())
					.isEqualTo(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
			});
	}

	private static Message<byte[]> subscribeMessage(String destination, Principal user) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination(destination);
		if (user != null) {
			accessor.setUser(user);
		}
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private static Authentication authMember(Long memberId) {
		AuthMember member = new AuthMember(memberId, Role.USER);
		return new UsernamePasswordAuthenticationToken(member, null, List.of());
	}
}
