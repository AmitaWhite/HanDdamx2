package com.white.handdam.board.converter;

import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPost;

public final class BoardPostConverter {

	private BoardPostConverter() {
	}

	public static BoardPostResponse toResponse(BoardPost post) {
		return new BoardPostResponse(
			post.getId(),
			post.getCreatorId(),
			post.getMemberId(),
			post.getType(),
			post.getContent(),
			post.getStatus(),
			post.getCreatedAt(),
			post.getUpdatedAt()
		);
	}
}
