package com.white.handdam.board.entity;

import com.white.handdam.global.entity.BaseCreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "board_post_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPostImage extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "board_post_id", nullable = false)
	private BoardPost boardPost;

	@Column(name = "url", nullable = false, length = 500)
	private String url;

	@Column(name = "storage_key", nullable = false, length = 500)
	private String storageKey;

	@Column(name = "original_name", length = 255)
	private String originalName;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "mime_type", length = 100)
	private String mimeType;

	@Column(name = "order_index", nullable = false)
	private int orderIndex;

	@Builder
	private BoardPostImage(
		BoardPost boardPost,
		String url,
		String storageKey,
		String originalName,
		Long fileSize,
		String mimeType,
		int orderIndex
	) {
		this.boardPost = boardPost;
		this.url = url;
		this.storageKey = storageKey;
		this.originalName = originalName;
		this.fileSize = fileSize;
		this.mimeType = mimeType;
		this.orderIndex = orderIndex;
	}
}
