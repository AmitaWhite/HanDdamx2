package com.white.handdam.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "chat_room_id", nullable = false)
	private Long chatRoomId;

	@Column(name = "sender_id", nullable = false)
	private Long senderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 50)
	private ChatMessageType type;

	@Column(name = "content", length = 2000)
	private String content;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Column(name = "image_storage_key", length = 500)
	private String imageStorageKey;

	@Column(name = "image_original_name", length = 255)
	private String imageOriginalName;

	@Column(name = "read_at")
	private Instant readAt;

	@Column(name = "sent_at", nullable = false, updatable = false)
	private Instant sentAt;

	@Builder
	private ChatMessage(
		Long chatRoomId,
		Long senderId,
		ChatMessageType type,
		String content,
		String imageUrl,
		String imageStorageKey,
		String imageOriginalName
	) {
		this.chatRoomId = chatRoomId;
		this.senderId = senderId;
		this.type = type;
		this.content = content;
		this.imageUrl = imageUrl;
		this.imageStorageKey = imageStorageKey;
		this.imageOriginalName = imageOriginalName;
	}

	@PrePersist
	void onCreate() {
		if (sentAt == null) {
			sentAt = Instant.now();
		}
	}
}
