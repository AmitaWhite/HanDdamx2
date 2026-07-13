package com.white.handdam.board.entity;

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
@Table(name = "board_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPost extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "creator_id", nullable = false)
	private Long creatorId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 50)
	private BoardPostType type;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 50)
	private BoardPostStatus status;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Builder
	private BoardPost(
		Long creatorId,
		Long memberId,
		String title,
		BoardPostType type,
		String content,
		BoardPostStatus status
	) {
		this.creatorId = creatorId;
		this.memberId = memberId;
		this.title = title;
		this.type = type;
		this.content = content;
		this.status = status == null ? BoardPostStatus.WAITING : status;
		this.deleted = false;
	}

	/**
	 * 공식 답변 전(WAITING) 게시글의 제목·유형·본문 수정.
	 */
	public void update(String title, BoardPostType type, String content) {
		this.title = title;
		this.type = type;
		this.content = content;
	}

	/**
	 * 소프트 삭제: is_deleted=true, deleted_at 기록.
	 */
	public void softDelete() {
		this.deleted = true;
		this.deletedAt = Instant.now();
	}
}
