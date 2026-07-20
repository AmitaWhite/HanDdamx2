import type { ReactNode } from "react";
import { createContext, useCallback, useContext, useState } from "react";

interface SubscriptionContextValue {
	isFreeSubscribed: (creatorId: string) => boolean;
	subscribeFree: (creatorId: string) => void;
	unsubscribe: (creatorId: string) => void;
}

const SubscriptionContext = createContext<SubscriptionContextValue | null>(null);

/** 세션 내에서만 유지되는 "무료 구독 중인 크리에이터" 목데이터 상태. 새로고침 시 초기화됨. */
export function SubscriptionProvider({ children }: { children: ReactNode }) {
	const [freeSubscribed, setFreeSubscribed] = useState<Set<string>>(new Set());

	const isFreeSubscribed = useCallback((creatorId: string) => freeSubscribed.has(creatorId), [freeSubscribed]);

	const subscribeFree = useCallback((creatorId: string) => {
		setFreeSubscribed((prev) => new Set(prev).add(creatorId));
	}, []);

	const unsubscribe = useCallback((creatorId: string) => {
		setFreeSubscribed((prev) => {
			const next = new Set(prev);
			next.delete(creatorId);
			return next;
		});
	}, []);

	return (
		<SubscriptionContext.Provider value={{ isFreeSubscribed, subscribeFree, unsubscribe }}>
			{children}
		</SubscriptionContext.Provider>
	);
}

// eslint-disable-next-line react-refresh/only-export-components
export function useSubscription() {
	const ctx = useContext(SubscriptionContext);
	if (!ctx) throw new Error("useSubscription 은 SubscriptionProvider 안에서만 사용할 수 있습니다.");
	return ctx;
}
