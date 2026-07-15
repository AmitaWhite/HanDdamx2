package com.white.handdam.board.converter;

import com.white.handdam.board.dto.response.BoardAnswerResponse;
import com.white.handdam.board.entity.BoardAnswer;

public final class BoardAnswerConverter {

	private BoardAnswerConverter() {
	}

	public static BoardAnswerResponse toResponse(BoardAnswer answer) {
		return new BoardAnswerResponse(
			answer.getId(),
			answer.getBoardPost().getId(),
			answer.getCreatorId(),
			answer.getContent(),
			answer.getCreatedAt(),
			answer.getUpdatedAt(),
			answer.getDeletedAt()
		);
	}
}
