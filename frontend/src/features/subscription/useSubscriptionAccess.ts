import { useEffect, useState } from "react";
import { useAuth } from "@/features/auth/AuthContext";
import { getSubscriptionStatus, hasActivePaidAccess } from "./subscriptionApi";

export interface SubscriptionAccess {
	loading: boolean;
	/** 로그인한 사용자가 이 크리에이터 본인인지 */
	isCreator: boolean;
	hasActivePaidSubscription: boolean;
}

/**
 * 특정 크리에이터 게시판에 대한 구독 접근 상태.
 * 호출부가 목적에 맞게 조합해서 쓴다:
 * - "글쓰기(질문)" 가능 여부: `!isCreator && hasActivePaidSubscription`
 * - "댓글쓰기" 가능 여부: `isCreator || hasActivePaidSubscription`
 */
export function useSubscriptionAccess(
	creatorId: number | null,
): SubscriptionAccess {
	const { user } = useAuth();
	const isCreator = !!user && creatorId !== null && user.memberId === creatorId;

	const [loading, setLoading] = useState(creatorId !== null && !isCreator);
	const [hasActivePaidSubscription, setHasActivePaidSubscription] =
		useState(false);

	useEffect(() => {
		// mock 모드(creatorId 없음) 또는 크리에이터 본인 — API 호출 없이 즉시 확정.
		// 본인 게시판은 백엔드가 항상 notSubscribed를 주므로 호출해도 결과가 같다.
		if (creatorId === null || isCreator) {
			setLoading(false);
			setHasActivePaidSubscription(false);
			return;
		}
		// 비로그인 — 구독 상태를 알 수 없으니 API 호출 없이 fail-closed.
		if (!user) {
			setLoading(false);
			setHasActivePaidSubscription(false);
			return;
		}

		let cancelled = false;
		setLoading(true);

		getSubscriptionStatus(creatorId)
			.then((res) => {
				if (!cancelled) setHasActivePaidSubscription(hasActivePaidAccess(res));
			})
			.catch(() => {
				if (!cancelled) setHasActivePaidSubscription(false);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
		// user?.memberId를 넣어 로그인/로그아웃·계정 전환 시 재조회한다(isCreator만으로는 감지 못하는 케이스가 있음).
	}, [creatorId, isCreator, user?.memberId]);

	return { loading, isCreator, hasActivePaidSubscription };
}
