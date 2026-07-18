import { useState } from "react";
import { ApiError } from "@/lib/api";

/**
 * 폼 제출의 loading/에러 처리 골격(setLoading→try/await→catch→finally).
 * 성공 후 navigate, 사전 검증 같은 페이지별 로직은 호출부의 action 함수 안에 둔다.
 */
export function useSubmitState(fallbackMessage: string) {
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);

	async function run<T>(action: () => Promise<T>): Promise<T | undefined> {
		setError(null);
		setLoading(true);
		try {
			return await action();
		} catch (err) {
			setError(err instanceof ApiError ? err.message : fallbackMessage);
			return undefined;
		} finally {
			setLoading(false);
		}
	}

	return { loading, error, setError, run };
}
