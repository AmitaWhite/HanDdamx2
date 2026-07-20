import { http, unwrap } from "@/lib/api";

/**
 * 백엔드 SubscriptionLevel / SubscriptionStatus
 * (com.white.handdam.subscription.entity.SubscriptionLevel / SubscriptionStatus)
 */
export type SubscriptionLevel = "FREE" | "PAID";
export type SubscriptionStatus = "ACTIVE" | "CANCEL_SCHEDULED";

/**
 * 백엔드 SubscriptionStatusResponse 와 1:1.
 * `subscribed`는 구독 row가 존재하기만 하면 true — 무료/만료 구독자도 포함되므로
 * 유료 접근 판정엔 단독으로 쓰지 말고 반드시 {@link hasActivePaidAccess}를 통해야 한다.
 */
export interface SubscriptionStatusResponse {
	creatorId: number;
	subscribed: boolean;
	subscriptionLevel: SubscriptionLevel | null;
	status: SubscriptionStatus | null;
	startedAt: string | null;
	currentPeriodEndAt: string | null;
}

/**
 * 구독 상태 조회.
 * 백엔드: GET /api/creators/{creatorId}/subscription-status
 * 크리에이터 본인이 자기 게시판을 조회하면 항상 notSubscribed로 응답한다(백엔드 사양).
 */
export function getSubscriptionStatus(creatorId: number) {
	return unwrap<SubscriptionStatusResponse>(
		http.get(`/creators/${creatorId}/subscription-status`),
	);
}

/**
 * "지금 유료 권한이 있는지" 판정 — 백엔드 SubscriptionPaidSubscriptionChecker와 동일 로직.
 * subscriptionLevel=PAID AND status가 ACTIVE/CANCEL_SCHEDULED AND currentPeriodEndAt이 아직 안 지남.
 */
export function hasActivePaidAccess(res: SubscriptionStatusResponse): boolean {
	if (res.subscriptionLevel !== "PAID") return false;
	if (res.status !== "ACTIVE" && res.status !== "CANCEL_SCHEDULED")
		return false;
	if (!res.currentPeriodEndAt) return false;
	return Date.now() < new Date(res.currentPeriodEndAt).getTime();
}
