package com.white.handdam.board.service;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link PaidSubscriptionChecker} 의 실제 구현체.
 *
 * <p>결제(Toss confirm) 성공 시 DB {@code subscription} 에 PAID 구독이 쌓이고,
 * 보드·채팅은 이 클래스로 그 레코드를 읽어 "지금 유료 권한이 있는지" 판별한다.
 *
 * <p>판별 조건 (둘 다 만족해야 true):
 * <ol>
 *   <li>{@code subscription_level = PAID}</li>
 *   <li>{@code status = ACTIVE} 또는 {@code CANCEL_SCHEDULED}</li>
 * </ol>
 *
 * <p>{@code CANCEL_SCHEDULED} 를 허용하는 이유:
 * 해지 예약만 한 상태라도 유료 기간이 끝나기 전까지는 권한을 유지해야 한다.
 *
 * <p>사용처:
 * <ul>
 *   <li>보드 — 포스트/댓글 접근·작성 ({@code BoardPostService})</li>
 *   <li>채팅 — 방 생성·메시지 전송 ({@code ChatRoomService}, {@code ChatMessageService})</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SubscriptionPaidSubscriptionChecker implements PaidSubscriptionChecker {

	/** subscriber_id + creator_id 로 구독 1건을 조회한다. */
	private final SubscriptionRepository subscriptionRepository;

	/**
	 * 회원({@code memberId})이 크리에이터({@code creatorId})에 대해
	 * 활성 유료 구독을 갖고 있는지 반환한다.
	 *
	 * @param memberId  구독자 MEMBER.id (null 이면 false)
	 * @param creatorId 크리에이터 MEMBER.id (null 이면 false)
	 * @return PAID + (ACTIVE|CANCEL_SCHEDULED) 이면 true, 없으면 false
	 */
	@Override
	public boolean hasActivePaidSubscription(Long memberId, Long creatorId) {
		// 로그인 안 됐거나 대상 크리에이터가 없으면 구독 권한 없음
		if (memberId == null || creatorId == null) {
			return false;
		}

		// 1) (구독자, 크리에이터) 쌍으로 subscription 조회
		// 2) 있으면 isActivePaid 로 PAID + 활성 상태인지 필터
		// 3) 통과한 Optional 이 있으면 true
		return subscriptionRepository.findBySubscriberIdAndCreatorId(memberId, creatorId)
			.filter(this::isActivePaid)
			.isPresent();
	}

	/**
	 * 단일 구독 행이 "지금 유료 접근 권한이 있는 상태"인지 검사한다.
	 *
	 * <ul>
	 *   <li>FREE → false (무료 구독만으로는 보드·채팅 불가)</li>
	 *   <li>PAID + ACTIVE → true</li>
	 *   <li>PAID + CANCEL_SCHEDULED → true (기간 만료 전까지 유지)</li>
	 *   <li>그 외 status → false</li>
	 * </ul>
	 */
	private boolean isActivePaid(Subscription subscription) {
		if (!subscription.isPaid()) {
			return false;
		}
		SubscriptionStatus status = subscription.getStatus();
		return status == SubscriptionStatus.ACTIVE
			|| status == SubscriptionStatus.CANCEL_SCHEDULED;
	}
}
