package com.white.handdam.board.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board_answer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardAnswer extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	//FetchType.LAZY: 필요할 때 로드
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "board_post_id", nullable = false, unique = true)
	private BoardPost boardPost;

	@Column(name = "creator_id", nullable = false)
	private Long creatorId;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Builder
	private BoardAnswer(BoardPost boardPost, Long creatorId, String content) {
		this.boardPost = boardPost;
		this.creatorId = creatorId;
		this.content = content;
		this.deleted = false;
	}

	public void updateContent(String content) {
		this.content = content;
	}

	public void softDelete() {
		this.deleted = true;
		this.deletedAt = Instant.now();
	}

	/**
	 * 소프트 삭제된 공식 답변을 복구해 재등록한다.
	 * UNIQUE(board_post_id) 때문에 새 INSERT 대신 기존 행을 되살린다.
	 */
	public void restore(String content, Long creatorId) {
		this.content = content;
		this.creatorId = creatorId;
		this.deleted = false;
		this.deletedAt = null;
	}
}
