package com.white.handdam.board.service;

import org.springframework.stereotype.Component;

/**
 * SUBSCRIPTION 도메인 연동 전 임시 구현.
 * 실제 구현에서는 ERD 기준 활성 PAID 구독 존재 여부를 조회한다.
 */
@Component
public class StubPaidSubscriptionChecker implements PaidSubscriptionChecker {

	@Override
	public boolean hasActivePaidSubscription(Long memberId, Long creatorId) {
		// TODO: SUBSCRIPTION (PAID + ACTIVE/CANCEL_SCHEDULED) 조회로 교체
		return false;
	}
}
