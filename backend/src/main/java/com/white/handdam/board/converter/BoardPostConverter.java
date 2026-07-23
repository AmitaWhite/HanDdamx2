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
		return toResponse(post, List.of(), "알 수 없음");
	}

	public static BoardPostResponse toResponse(BoardPost post, List<BoardPostImage> images) {
		return toResponse(post, images, "알 수 없음");
	}

	public static BoardPostResponse toResponse(BoardPost post, List<BoardPostImage> images, String memberNickname) {
		return new BoardPostResponse(
			post.getId(),
			post.getCreatorId(),
			post.getMemberId(),
			memberNickname,
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
