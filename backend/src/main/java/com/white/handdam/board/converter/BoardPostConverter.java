package com.white.handdam.board.converter;

import com.white.handdam.board.dto.response.BoardPostImageResponse;
import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.entity.BoardPostImage;
import java.util.List;

public final class BoardPostConverter {

	private BoardPostConverter() {
	}

	public static BoardPostResponse toResponse(BoardPost post) {
		return toResponse(post, List.of());
	}

	public static BoardPostResponse toResponse(BoardPost post, List<BoardPostImage> images) {
		return new BoardPostResponse(
			post.getId(),
			post.getCreatorId(),
			post.getMemberId(),
			post.getTitle(),
			post.getType(),
			post.getContent(),
			post.getStatus(),
			post.getCreatedAt(),
			post.getUpdatedAt(),
			post.getDeletedAt(),
			images.stream().map(BoardPostConverter::toImageResponse).toList()
		);
	}

	public static BoardPostImageResponse toImageResponse(BoardPostImage image) {
		return new BoardPostImageResponse(
			image.getId(),
			image.getUrl(),
			image.getStorageKey(),
			image.getOriginalName(),
			image.getFileSize(),
			image.getMimeType(),
			image.getOrderIndex(),
			image.getCreatedAt()
		);
	}
}
