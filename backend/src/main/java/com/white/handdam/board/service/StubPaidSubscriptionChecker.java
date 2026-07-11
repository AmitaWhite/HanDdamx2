package com.white.handdam.board.service;

import org.springframework.stereotype.Component;

/**
 * SUBSCRIPTION 도메인 연동 전 임시 구현.
 *
 * <p>실제 구현에서는 SUBSCRIPTION에서
 * (subscriber_id = memberId, creator_id = creatorId)인 활성 유료 구독 존재 여부를 조회한다.
 */
@Component
public class StubPaidSubscriptionChecker implements PaidSubscriptionChecker {

	@Override
	public boolean hasActivePaidSubscription(Long memberId, Long creatorId) {
		// TODO: SUBSCRIPTION 활성 유료 구독 조회로 교체
		return false;
	}
}
