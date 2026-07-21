import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
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
	withCredentials: true,
});

const ACCESS_TOKEN_KEY = "handdam.accessToken";

export const tokenStore = {
	get: () => localStorage.getItem(ACCESS_TOKEN_KEY),
	set: (t: string) => localStorage.setItem(ACCESS_TOKEN_KEY, t),
	clear: () => localStorage.removeItem(ACCESS_TOKEN_KEY),
};

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshPromise: Promise<string> | null = null;

export interface JwtClaims {
	/** subject — memberId 문자열 */
	sub?: string;
	role?: string;
	exp?: number;
}

/** JWT payload(가운데 세그먼트)를 base64url 디코드해서 클레임 객체로 반환. 서명 검증은 하지 않음(클라이언트 표시용). */
export function decodeJwtPayload(token: string): JwtClaims | null {
	try {
		const payload = token.split(".")[1];
		if (!payload) return null;
		const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
		const padded = normalized.padEnd(
			normalized.length + ((4 - (normalized.length % 4)) % 4),
			"=",
		);
		return JSON.parse(atob(padded)) as JwtClaims;
	} catch {
		return null;
	}
}

/** access token 이 없거나 skewMs 이내 만료면 true */
export function isAccessTokenExpiringSoon(skewMs = 60_000): boolean {
	const token = tokenStore.get();
	if (!token) return true;
	const claims = decodeJwtPayload(token);
	if (!claims?.exp) return false;
	return claims.exp * 1000 <= Date.now() + skewMs;
}

/**
 * httpOnly refresh 쿠키로 access token 재발급.
 * authApi 를 거치지 않아 interceptor↔unwrap 순환을 피한다.
 */
export async function refreshAccessToken(): Promise<string> {
	const { data: body } = await axios.post<
		ApiResponse<{ accessToken: string }>
	>(`${apiBaseUrl}/auth/token/refresh`, {}, { withCredentials: true });
	const accessToken = body.data?.accessToken;
	if (!body.success || !accessToken) {
		throw new ApiError(
			body.error?.code ?? "UNAUTHORIZED",
			body.error?.message ?? "로그인이 만료되었습니다. 다시 로그인해 주세요.",
		);
	}
	tokenStore.set(accessToken);
	return accessToken;
}

/**
 * multipart 업로드 직전에 호출.
 * 401 재시도 시 FormData 가 비는 환경을 피하기 위해, 만료 임박이면 미리 갱신한다.
 */
export async function ensureFreshAccessToken(): Promise<void> {
	if (!isAccessTokenExpiringSoon()) return;
	if (!refreshPromise) {
		refreshPromise = refreshAccessToken().finally(() => {
			refreshPromise = null;
		});
	}
	await refreshPromise;
}

function isAuthEndpoint(url?: string): boolean {
	if (!url) return false;
	return (
		url.includes("/auth/login") ||
		url.includes("/auth/token/refresh") ||
		url.includes("/auth/logout")
	);
}

http.interceptors.request.use((config) => {
	const token = tokenStore.get();
	if (token) {
		config.headers.set("Authorization", `Bearer ${token}`);
	}
	if (typeof FormData !== "undefined" && config.data instanceof FormData) {
		config.headers.delete("Content-Type");
	}
	return config;
});

http.interceptors.response.use(
	(res) => res,
	async (error: AxiosError<ApiResponse<unknown>>) => {
		const original = error.config as RetriableConfig | undefined;
		if (
			error.response?.status !== 401 ||
			!original ||
			original._retry ||
			isAuthEndpoint(original.url)
		) {
			if (error.response?.status === 401 && !isAuthEndpoint(original?.url)) {
				tokenStore.clear();
			}
			return Promise.reject(error);
		}

		original._retry = true;
		try {
			if (!refreshPromise) {
				refreshPromise = refreshAccessToken().finally(() => {
					refreshPromise = null;
				});
			}
			const newToken = await refreshPromise;
			original.headers.set("Authorization", `Bearer ${newToken}`);
			return http.request(original);
		} catch {
			tokenStore.clear();
			return Promise.reject(error);
		}
	},
);

export class ApiError extends Error {
	constructor(
		public code: string,
		message: string,
	) {
		super(message);
		this.name = "ApiError";
	}
}

export function asApiError(
	err: unknown,
	fallback = "요청에 실패했습니다.",
): ApiError {
	if (err instanceof ApiError) return err;
	if (axios.isAxiosError<ApiResponse<unknown>>(err)) {
		if (err.response?.status === 401) {
			return new ApiError(
				err.response.data?.error?.code ?? "UNAUTHORIZED",
				err.response.data?.error?.message ??
					"로그인이 만료되었습니다. 다시 로그인해 주세요.",
			);
		}
		return new ApiError(
			err.response?.data?.error?.code ?? "UNKNOWN",
			err.response?.data?.error?.message ?? fallback,
		);
	}
	return new ApiError("UNKNOWN", fallback);
}

export async function unwrap<T>(
	promise: Promise<{ data: ApiResponse<T> }>,
): Promise<T> {
	try {
		const { data: body } = await promise;
		if (!body.success || body.data === null) {
			throw new ApiError(
				body.error?.code ?? "UNKNOWN",
				body.error?.message ?? "요청에 실패했습니다.",
			);
		}
		return body.data;
	} catch (err) {
		throw asApiError(err);
	}
}

export async function unwrapVoid(
	promise: Promise<{ data: ApiResponse<unknown> }>,
): Promise<void> {
	try {
		const { data: body } = await promise;
		if (!body.success) {
			throw new ApiError(
				body.error?.code ?? "UNKNOWN",
				body.error?.message ?? "요청에 실패했습니다.",
			);
		}
	} catch (err) {
		throw asApiError(err);
	}
}
