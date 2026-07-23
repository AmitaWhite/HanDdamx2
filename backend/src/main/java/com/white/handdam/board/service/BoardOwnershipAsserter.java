package com.white.handdam.board.service;

import com.white.handdam.board.exception.BoardErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.ErrorCode;

/**
 * 게시판 도메인 공통 소유자(작성자) 권한 검사.
 *
 * <p>게시글·댓글·답변 등에서 반복되는
 * 「로그인 여부 + ownerId == requesterId」 패턴을 한곳으로 모은다.
 */
public final class BoardOwnershipAsserter {

	private BoardOwnershipAsserter() {
	}

	/**
	 * @param ownerId       리소스 소유자(작성자) member id
	 * @param requesterId   요청자 member id
	 * @param forbiddenCode 소유자가 아닐 때 던질 에러 코드
	 */
	public static void assertOwner(Long ownerId, Long requesterId, ErrorCode forbiddenCode) {
		if (requesterId == null) {
			throw new CustomException(BoardErrorCode.BOARD_LOGIN_REQUIRED);
		}
		if (requesterId.equals(ownerId)) {
			return;
		}
		throw new CustomException(forbiddenCode);
	}
}
