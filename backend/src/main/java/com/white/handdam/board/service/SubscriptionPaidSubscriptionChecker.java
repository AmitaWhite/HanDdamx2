package com.white.handdam.board.service;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link PaidSubscriptionChecker} 의 실제 구현체.
 *
 * <p>결제(Toss confirm) 성공 시 DB {@code subscription} 에 PAID 구독이 쌓이고,
 * 보드·채팅은 이 클래스로 그 레코드를 읽어 "지금 유료 권한이 있는지" 판별한다.
 *
 * <p>판별 조건 (모두 만족해야 true):
 * <ol>
 *   <li>{@code subscription_level = PAID}</li>
 *   <li>{@code status = ACTIVE} 또는 {@code CANCEL_SCHEDULED}</li>
 *   <li>{@code now < current_period_end_at} (유료 기간 내)</li>
 * </ol>
 *
 * <p>{@code CANCEL_SCHEDULED} 라도 기간이 끝나기 전까지만 권한을 유지한다.
 * 기간이 지났으면 status가 남아 있어도 false.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionPaidSubscriptionChecker implements PaidSubscriptionChecker {

	private final SubscriptionRepository subscriptionRepository;

	@Override
	public boolean hasActivePaidSubscription(Long memberId, Long creatorId) {
		if (memberId == null || creatorId == null) {
			return false;
		}

		Instant now = Instant.now();
		return subscriptionRepository.findBySubscriberIdAndCreatorId(memberId, creatorId)
			.filter(subscription -> isActivePaidWithinPeriod(subscription, now))
			.isPresent();
	}

	/**
	 *  보드 쪽 포트에서 기간까지 판별한다.
	 */
	private boolean isActivePaidWithinPeriod(Subscription subscription, Instant now) {
		if (!subscription.isPaid()) {
			return false;
		}
		SubscriptionStatus status = subscription.getStatus();
		if (status != SubscriptionStatus.ACTIVE && status != SubscriptionStatus.CANCEL_SCHEDULED) {
			return false;
		}
		Instant periodEndAt = subscription.getCurrentPeriodEndAt();
		if (periodEndAt == null || now == null) {
			return false;
		}
		return now.isBefore(periodEndAt);
	}
}
