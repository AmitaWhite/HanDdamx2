package com.white.handdam.board.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardComment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "board_post_id", nullable = false)
	private BoardPost boardPost;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id")
	private BoardComment parentComment;

	@Column(name = "depth", nullable = false)
	private short depth;

	@Column(name = "content", nullable = false, length = 1000)
	private String content;

	@Column(name = "is_deleted", nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Builder
	private BoardComment(
		BoardPost boardPost,
		Long memberId,
		BoardComment parentComment,
		short depth,
		String content
	) {
		this.boardPost = boardPost;
		this.memberId = memberId;
		this.parentComment = parentComment;
		this.depth = depth;
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
}
