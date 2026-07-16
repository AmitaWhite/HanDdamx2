import axios, { type AxiosError } from "axios";
import type { ApiResponse } from "./types";

/**
 * 개발 시엔 vite 프록시(/api → :8080)를 태우고,
 * 배포 시엔 VITE_API_BASE_URL 을 사용한다.
 */
const baseURL = import.meta.env.VITE_API_BASE_URL || "/api";

export const http = axios.create({
	baseURL,
	headers: { "Content-Type": "application/json" },
});

// --- 토큰 관리 (임시: localStorage. 추후 auth 스토어로 이동) ---
const ACCESS_TOKEN_KEY = "handdam.accessToken";

export const tokenStore = {
	get: () => localStorage.getItem(ACCESS_TOKEN_KEY),
	set: (t: string) => localStorage.setItem(ACCESS_TOKEN_KEY, t),
	clear: () => localStorage.removeItem(ACCESS_TOKEN_KEY),
};

// 요청 인터셉터: JWT 자동 첨부
http.interceptors.request.use((config) => {
	const token = tokenStore.get();
	if (token) {
		config.headers.Authorization = `Bearer ${token}`;
	}
	return config;
});

// 응답 인터셉터: 401 처리 훅 자리 (리프레시 로직은 auth 붙일 때 구현)
http.interceptors.response.use(
	(res) => res,
	(error: AxiosError<ApiResponse<unknown>>) => {
		if (error.response?.status === 401) {
			// TODO: refresh token 재발급 흐름 연결
			tokenStore.clear();
		}
		return Promise.reject(error);
	},
);

/**
 * ApiResponse<T> 를 벗겨 data 만 반환하는 헬퍼.
 * 실패 시 error 를 throw 하여 호출부에서 try/catch 로 처리.
 */
export async function unwrap<T>(
	promise: Promise<{ data: ApiResponse<T> }>,
): Promise<T> {
	const { data: body } = await promise;
	if (!body.success || body.data === null) {
		throw new ApiError(
			body.error?.code ?? "UNKNOWN",
			body.error?.message ?? "요청에 실패했습니다.",
		);
	}
	return body.data;
}

export class ApiError extends Error {
	constructor(
		public code: string,
		message: string,
	) {
		super(message);
		this.name = "ApiError";
	}
}
