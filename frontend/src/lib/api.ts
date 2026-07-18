import axios, { type AxiosError } from "axios";
import type { ApiResponse } from "./types";

/**
 * 개발 시엔 VITE_API_BASE_URL(예: http://localhost:8080/api)로 직접 호출한다.
 * refresh token 이 httpOnly 쿠키라 withCredentials 필수(백엔드 CORS allowCredentials=true).
 */
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "/api";

/** 백엔드 origin (OAuth 풀페이지 리다이렉트 등 /api 밖 경로용). 예: http://localhost:8080 */
export const backendOrigin = apiBaseUrl.replace(/\/api\/?$/, "");

export const http = axios.create({
	baseURL: apiBaseUrl,
	headers: { "Content-Type": "application/json" },
	withCredentials: true,
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

// 응답 인터셉터: 401 처리 훅 자리 (silent refresh 는 후속 과제)
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
 * 실패 시 ApiError 를 throw 하여 호출부에서 try/catch 로 처리.
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

/**
 * noContent(data=null) 응답용. unwrap()과 달리 data===null 은 정상 허용하되,
 * success:false 는 여전히 ApiError 로 변환한다(HTTP 200 이면서 success:false 인 경우 대비).
 */
export async function unwrapVoid(
	promise: Promise<{ data: ApiResponse<unknown> }>,
): Promise<void> {
	const { data: body } = await promise;
	if (!body.success) {
		throw new ApiError(
			body.error?.code ?? "UNKNOWN",
			body.error?.message ?? "요청에 실패했습니다.",
		);
	}
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
