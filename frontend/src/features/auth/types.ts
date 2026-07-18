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

/** 앱 전역에서 쓰는 로그인 사용자 정보 */
export interface AuthUser {
	memberId: number;
	nickname: string;
	role: Role;
}
