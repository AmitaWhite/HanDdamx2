package com.white.handdam.board.service;

import com.white.handdam.board.converter.BoardPostConverter;
import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.repository.BoardPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPostService {

	private final BoardPostRepository boardPostRepository;
	private final PaidSubscriptionChecker paidSubscriptionChecker;

	public Page<BoardPostResponse> getPostsByCreator(
		Long creatorId,
		Long requesterId,
		BoardPostType type,
		BoardPostStatus status,
		Pageable pageable
	) {
		assertCanAccess(creatorId, requesterId);
		return boardPostRepository
			.findByCreator(creatorId, type, status, pageable)
			.map(BoardPostConverter::toResponse);
	}

	void assertCanAccess(Long creatorId, Long requesterId) {
		if (requesterId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인이 필요합니다.");
		}
		if (requesterId.equals(creatorId)) {
			return;
		}
		if (paidSubscriptionChecker.hasActivePaidSubscription(requesterId, creatorId)) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "유료 게시판에 접근할 권한이 없습니다.");
	}
}
