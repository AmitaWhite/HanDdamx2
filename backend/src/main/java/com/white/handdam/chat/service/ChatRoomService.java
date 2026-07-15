package com.white.handdam.chat.service;

import com.white.handdam.board.service.PaidSubscriptionChecker;
import com.white.handdam.chat.converter.ChatRoomConverter;
import com.white.handdam.chat.dto.response.ChatRoomResponse;
import com.white.handdam.chat.entity.ChatRoom;
import com.white.handdam.chat.exception.ChatErrorCode;
import com.white.handdam.chat.repository.ChatRoomRepository;
import com.white.handdam.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

	private final ChatRoomRepository chatRoomRepository;
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
	 * 채팅방 생성/조회 결과 (서비스 → 컨트롤러 전달용).
	 *
	 * @param room    채팅방 응답 DTO
	 * @param created true면 이번 요청에서 신규 생성(201), false면 기존 방 반환(200)
	 */
	public record CreateOrGetResult(ChatRoomResponse room, boolean created) {
	}
}
