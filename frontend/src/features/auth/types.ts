/** 백엔드 인증 계약 (com.white.handdam.auth.dto.*) 와 1:1 */

export type Role = "USER" | "CREATOR" | "ADMIN";

export interface LoginRequest {
	email: string;
	password: string;
}

/** POST /api/auth/login 응답 */
export interface LoginResponse {
	accessToken: string;
	memberId: number;
	nickname: string;
	role: Role;
}

/** POST /api/auth/token/refresh 응답 */
export interface TokenRefreshResponse {
	accessToken: string;
}

/** POST /api/auth/signup 요청 */
export interface SignupRequest {
	email: string;
	password: string;
	nickname: string;
}

/** POST /api/auth/signup 응답 */
export interface SignupResponse {
	memberId: number;
	email: string;
	nickname: string;
}

/** GET /api/auth/{email,nickname}-availability 응답 */
export interface AvailabilityResponse {
	available: boolean;
}

/**
 * 앱 전역에서 쓰는 로그인 사용자 정보.
 * memberId/role은 JWT에서 즉시(동기) 복원되지만, nickname은 JWT에 없어 `/members/me`로 비동기 보강되기 전까진 비어있을 수 있다.
 */
export interface AuthUser {
	memberId: number;
	nickname?: string;
	profileImageUrl?: string | null;
	role: Role;
}
