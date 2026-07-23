package com.white.handdam.chat.repository;

import com.white.handdam.chat.entity.ChatMessage;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	Page<ChatMessage> findByChatRoomId(Long chatRoomId, Pageable pageable);

	/** 방별 sentAt 최대인 메시지 조회 */
	@Query("""
		SELECT m FROM ChatMessage m
		WHERE m.chatRoomId IN :roomIds
		  AND m.sentAt = (
		    SELECT MAX(m2.sentAt) FROM ChatMessage m2
		    WHERE m2.chatRoomId = m.chatRoomId
		  )
		""")
	List<ChatMessage> findLatestByChatRoomIdIn(@Param("roomIds") Collection<Long> roomIds);

	/** 방별 미읽음 개수 조회 (상대가 보낸 메시지 중 readAt IS NULL) */
	@Query("""
		SELECT m.chatRoomId, COUNT(m)
		FROM ChatMessage m
		WHERE m.chatRoomId IN :roomIds
		  AND m.senderId <> :memberId
		  AND m.readAt IS NULL
		GROUP BY m.chatRoomId
		""")
	List<Object[]> countUnreadByChatRoomIdIn(
		@Param("roomIds") Collection<Long> roomIds,
		@Param("memberId") Long memberId
	);

	/**
	 * 상대방이 보낸 미확인 메시지를 일괄 읽음 처리.
	 * readerId 본인이 보낸 메시지는 제외한다.
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		UPDATE ChatMessage m
		SET m.readAt = :readAt
		WHERE m.chatRoomId = :chatRoomId
		  AND m.senderId <> :readerId
		  AND m.readAt IS NULL
		""")
	int markOpponentMessagesAsRead(
		@Param("chatRoomId") Long chatRoomId,
		@Param("readerId") Long readerId,
		@Param("readAt") Instant readAt
	);
}
