package com.white.handdam.board.exception;

import com.white.handdam.global.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BoardErrorCode implements ErrorCode {

	BOARD_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	BOARD_SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "유료 구독자만 접근할 수 있는 콘텐츠입니다."),

	BOARD_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
	BOARD_POST_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "게시글을 수정할 권한이 없습니다."),
	BOARD_POST_ALREADY_ANSWERED(HttpStatus.CONFLICT, "공식 답변이 등록된 게시글은 수정할 수 없습니다."),
	BOARD_POST_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "업로드할 이미지가 없습니다."),
	BOARD_POST_IMAGE_ADD_NOT_ALLOWED(HttpStatus.CONFLICT, "공식 답변이 등록된 게시글은 이미지를 추가할 수 없습니다."),
	BOARD_POST_IMAGE_DELETE_NOT_ALLOWED(HttpStatus.CONFLICT, "공식 답변이 등록된 게시글은 이미지를 삭제할 수 없습니다."),
	BOARD_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다."),

	BOARD_ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "공식 답변을 찾을 수 없습니다."),
	BOARD_ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 공식 답변이 등록된 게시글입니다."),
	BOARD_ANSWER_CREATE_FORBIDDEN(HttpStatus.FORBIDDEN, "공식 답변을 작성할 권한이 없습니다."),
	BOARD_ANSWER_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "공식 답변을 수정할 권한이 없습니다."),

	BOARD_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
	BOARD_COMMENT_EDIT_FORBIDDEN(HttpStatus.FORBIDDEN, "댓글을 수정할 권한이 없습니다."),
	BOARD_COMMENT_REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 다시 답글을 달 수 없습니다.");

	private final HttpStatus status;
	private final String message;

	BoardErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
}
