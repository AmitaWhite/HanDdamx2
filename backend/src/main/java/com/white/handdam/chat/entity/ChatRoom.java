package com.white.handdam.chat.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "creator_id", nullable = false)
	private Long creatorId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 50)
	private ChatRoomStatus status;

	@Column(name = "closed_by")
	private Long closedBy;

	@Column(name = "closed_at")
	private Instant closedAt;

	@Column(name = "last_message_at")
	private Instant lastMessageAt;

	@Builder
	private ChatRoom(Long creatorId, Long memberId) {
		this.creatorId = creatorId;
		this.memberId = memberId;
		this.status = ChatRoomStatus.ACTIVE;
	}
}
