package com.white.handdam.chat.repository;

import com.white.handdam.chat.entity.ChatRoom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	Optional<ChatRoom> findByCreatorIdAndMemberId(Long creatorId, Long memberId);
}
