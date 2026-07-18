package com.white.handdam.board.service;

/**
 * 유료 구독 여부 조회 포트.
 *
 * <p>ERD: MEMBER(구독자) N─M MEMBER(크리에이터) via SUBSCRIPTION.
 * 유료 게시판·채팅 접근 조건은 활성 유료 구독이다.
 * <ul>
 *   <li>subscriber_id = memberId</li>
 *   <li>creator_id = creatorId</li>
 *   <li>subscription_level = PAID</li>
 *   <li>status = ACTIVE 또는 CANCEL_SCHEDULED</li>
 *   <li>current_period_end_at 이 현재 시각보다 이후 (유료 기간 내)</li>
 * </ul>
 *
 * <p>구현체: {@link SubscriptionPaidSubscriptionChecker}
 */
public interface PaidSubscriptionChecker {

	/**
	 * @param memberId  구독자(MEMBER.id)
	 * @param creatorId 크리에이터(MEMBER.id)
	 * @return 해당 크리에이터에 대한 활성 유료 구독이 있으면 true
	 */
	boolean hasActivePaidSubscription(Long memberId, Long creatorId);
}
