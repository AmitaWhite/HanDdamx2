package com.white.handdam.board.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BoardErrorCode implements ErrorCode {

	BOARD_POST_ALREADY_ANSWERED(HttpStatus.CONFLICT, "공식 답변이 등록된 게시글은 수정할 수 없습니다."),
	BOARD_ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 공식 답변이 등록된 게시글입니다."),
	BOARD_POST_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "업로드할 이미지가 없습니다."),
	BOARD_COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 다시 답글을 달 수 없습니다.");

	private final HttpStatus status;
	private final String message;

	BoardErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
}
