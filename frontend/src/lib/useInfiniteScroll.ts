import { useEffect, useRef } from "react";

/**
 * sentinel 엘리먼트가 화면에 들어오면 onLoadMore를 호출한다.
 * 반환된 ref를 목록 맨 아래의 빈 엘리먼트에 붙여서 사용.
 */
export function useInfiniteScroll(onLoadMore: () => void, enabled: boolean) {
	const sentinelRef = useRef<HTMLDivElement>(null);
	const callbackRef = useRef(onLoadMore);
	callbackRef.current = onLoadMore;

	useEffect(() => {
		if (!enabled) return;
		const el = sentinelRef.current;
		if (!el) return;

		const observer = new IntersectionObserver(([entry]) => {
			if (entry.isIntersecting) callbackRef.current();
		});
		observer.observe(el);
		return () => observer.disconnect();
	}, [enabled]);

	return sentinelRef;
}
