package com.white.handdam.chat.repository;

import com.white.handdam.chat.entity.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByCreatorIdAndMemberId(Long creatorId, Long memberId);

	@Query("""
		SELECT r FROM ChatRoom r
		WHERE r.creatorId = :memberId OR r.memberId = :memberId
		ORDER BY r.lastMessageAt DESC NULLS LAST
		""")
	List<ChatRoom> findParticipatingOrderByLastMessageAtDesc(@Param("memberId") Long memberId);
}
