package com.white.handdam.chat.service;

import com.white.handdam.board.service.PaidSubscriptionChecker;
import com.white.handdam.chat.converter.ChatRoomConverter;
import com.white.handdam.chat.dto.response.ChatRoomListItemResponse;
import com.white.handdam.chat.dto.response.ChatRoomResponse;
import com.white.handdam.chat.entity.ChatMessage;
import com.white.handdam.chat.entity.ChatRoom;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.repository.ChatMessageRepository;
import com.white.handdam.chat.repository.ChatRoomRepository;
import com.white.handdam.global.exception.CustomException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final PaidSubscriptionChecker paidSubscriptionChecker;

	/**
	 * 채팅방 생성 또는 기존 방 반환 (CHAT-001 / LDJ-017).
	 *
	 * <pre>
	 * 1. 로그인 확인
	 * 2. 자기 자신과의 채팅 방지
	 * 3. 활성 유료 구독자만 허용 (아니면 403)
	 * 4. (creatorId, memberId) 기존 방 있으면 반환
	 * 5. 없으면 ACTIVE 채팅방 생성 후 반환
	 * </pre>
	 *
	 * @return result.created() == true 이면 신규 생성
	 */
	@Transactional
	public CreateOrGetResult createOrGetChatRoom(Long creatorId, Long requesterId) {
		if (requesterId == null) {
			throw new CustomException(ChatErrorCode.CHAT_LOGIN_REQUIRED);
		}
		if (requesterId.equals(creatorId)) {
			throw new CustomException(ChatErrorCode.CHAT_SELF_NOT_ALLOWED);
		}
		if (!paidSubscriptionChecker.hasActivePaidSubscription(requesterId, creatorId)) {
			throw new CustomException(ChatErrorCode.CHAT_SUBSCRIPTION_REQUIRED);
		}

		return chatRoomRepository.findByCreatorIdAndMemberId(creatorId, requesterId)
			.map(room -> new CreateOrGetResult(ChatRoomConverter.toResponse(room), false))
			.orElseGet(() -> {
				ChatRoom saved = chatRoomRepository.save(
					ChatRoom.builder()
						.creatorId(creatorId)
						.memberId(requesterId)
						.build()
				);
				return new CreateOrGetResult(ChatRoomConverter.toResponse(saved), true);
			});
	}

	/**
	 * 참여 채팅방 목록 조회 (CHAT-005 / LDJ-018).
	 *
	 * <pre>
	 * 1. 로그인 확인
	 * 2. creator_id 또는 member_id 가 요청자인 방 조회
	 * 3. last_message_at 최신순 (NULL 은 뒤로)
	 * 4. 방별 마지막 메시지·상대 미읽음 수 함께 반환
	 * </pre>
	 */
	public List<ChatRoomListItemResponse> getMyChatRooms(Long memberId) {
		if (memberId == null) {
			throw new CustomException(ChatErrorCode.CHAT_LOGIN_REQUIRED);
		}

		List<ChatRoom> rooms = chatRoomRepository.findParticipatingOrderByLastMessageAtDesc(memberId);
		if (rooms.isEmpty()) {
			return List.of();
		}

		// 방마다 메시지/미읽음을 따로 조회하면 N+1이 되므로,
		// 방 ID 목록으로 한 번에 조회한 뒤 Map으로 붙여 쓴다.
		List<Long> roomIds = rooms.stream().map(ChatRoom::getId).toList();

		// 방ID → 해당 방의 마지막 메시지
		// findLatestByChatRoomIdIn: 방별 sentAt 최대인 메시지 조회
		// toMap 세 번째 인자: 같은 방에 sentAt이 동률이면 id가 큰 메시지를 채택
		Map<Long, ChatMessage> lastMessages = chatMessageRepository.findLatestByChatRoomIdIn(roomIds)
			.stream()
			.collect(Collectors.toMap(
				//chatRoomId =map의 키
				ChatMessage::getChatRoomId,
				//Map의 Value
				Function.identity(),
				//메세지 id가 더큰걸 선택함
				(left, right) -> left.getId() >= right.getId() ? left : right
			));

		// 방ID → 미읽음 개수
		// 상대가 보낸 메시지 중 readAt IS NULL 인 것만 집계 (내가 보낸 것 제외)
		// 미읽음이 0인 방은 결과 행이 없으므로 아래에서 getOrDefault(..., 0L) 처리
		Map<Long, Long> unreadCounts = toUnreadCountMap(
			chatMessageRepository.countUnreadByChatRoomIdIn(roomIds, memberId)
		);

		// 방 목록 순서(lastMessageAt 최신순)를 유지한 채
		// 마지막 메시지 미리보기 + 미읽음 수를 붙여 목록 DTO로 변환
		return rooms.stream()
			.map(room -> ChatRoomConverter.toListItem(
				room,
				lastMessages.get(room.getId()), // 메시지 없으면 null → 미리보기 없음
				unreadCounts.getOrDefault(room.getId(), 0L)
			))
			.toList();
	}

	/**
	 * 채팅방 상세·상태 조회 (CHAT-009 / LDJ-019).
	 *
	 * <pre>
	 * 1. 로그인 확인
	 * 2. 채팅방 존재 확인 (없으면 404)
	 * 3. creator 또는 member 참여자만 허용 (아니면 403)
	 * 4. ACTIVE/CLOSED 상태와 종료 정보 포함해 반환
	 *    (구독 만료여도 조회는 허용 — 전송 차단은 메시지 API 책임)
	 * </pre>
	 */
	public ChatRoomResponse getChatRoom(Long chatRoomId, Long memberId) {
		if (memberId == null) {
			throw new CustomException(ChatErrorCode.CHAT_LOGIN_REQUIRED);
		}

		ChatRoom room = chatRoomRepository.findById(chatRoomId)
			.orElseThrow(() -> new CustomException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

		if (!room.isParticipant(memberId)) {
			throw new CustomException(ChatErrorCode.CHAT_NOT_PARTICIPANT);
		}

		return ChatRoomConverter.toResponse(room);
	}

	/**
	 * 미읽음 집계 결과({@code List<Object[]>} = [chatRoomId, count])를
	 * 방ID → 개수 Map으로 변환한다.
	 */
	private static Map<Long, Long> toUnreadCountMap(List<Object[]> rows) {
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyMap();
		}
		return rows.stream().collect(Collectors.toMap(
			row -> (Long) row[0], // chatRoomId
			row -> (Long) row[1]  // unread count
		));
	}

	/**
	 * 채팅방 생성/조회 결과 (서비스 → 컨트롤러 전달용).
	 *
	 * @param room    채팅방 응답 DTO
	 * @param created true면 이번 요청에서 신규 생성(201), false면 기존 방 반환(200)
	 */
	public record CreateOrGetResult(ChatRoomResponse room, boolean created) {
	}
}
