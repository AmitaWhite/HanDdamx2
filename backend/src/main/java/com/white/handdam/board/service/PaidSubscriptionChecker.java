package com.white.handdam.board.service;

/**
 * 유료 구독 여부 조회 포트.
 *
 * <p>관계: MEMBER(구독자) N─M MEMBER(크리에이터) via SUBSCRIPTION.
 * PAYMENT가 아니라 SUBSCRIPTION의 활성 유료 구독 여부를 본다.
 *
 * <p>구독 도메인 구현 후 SUBSCRIPTION 테이블을 조회하는 구현체로 교체한다.
 */
public interface PaidSubscriptionChecker {

	/**
	 * @param memberId  구독자(MEMBER.id)
	 * @param creatorId 크리에이터(MEMBER.id)
	 * @return 해당 크리에이터에 대한 활성 유료 구독이 있으면 true
	 */
	boolean hasActivePaidSubscription(Long memberId, Long creatorId);
}
