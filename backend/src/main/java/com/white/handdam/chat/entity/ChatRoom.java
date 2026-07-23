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
	//DB가 ID번호를 자동으로 만들어 준다
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

	/** 크리에이터 또는 멤버이면 참여자 */
	public boolean isParticipant(Long memberId) {
		if (memberId == null) {
			return false;
		}
		return memberId.equals(creatorId) || memberId.equals(this.memberId);
	}

	public boolean isActive() {
		return status == ChatRoomStatus.ACTIVE;
	}

	/** 구독자(member) 측 참여자인지 */
	public boolean isSubscriber(Long memberId) {
		return memberId != null && memberId.equals(this.memberId);
	}

	public void updateLastMessageAt(Instant sentAt) {
		this.lastMessageAt = sentAt;
	}

	/**
	 * 채팅방 종료 → 읽기 전용(CLOSED).
	 * 이미 종료된 방이면 false 반환.
	 */
	public boolean close(Long closedBy) {
		if (!isActive()) {
			return false;
		}
		this.status = ChatRoomStatus.CLOSED;
		this.closedBy = closedBy;
		this.closedAt = Instant.now();
		return true;
	}

	/**
	 * 종료된 방을 다시 활성화(ACTIVE)한다.
	 * closedBy·closedAt 은 초기화 — 이전 대화 내용은 그대로 유지된다.
	 * 이미 ACTIVE 면 false 반환.
	 */
	public boolean reopen() {
		if (isActive()) {
			return false;
		}
		this.status = ChatRoomStatus.ACTIVE;
		this.closedBy = null;
		this.closedAt = null;
		return true;
	}
}
