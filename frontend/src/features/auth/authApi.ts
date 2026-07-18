import { http, unwrap } from "@/lib/api";
import type { LoginResponse, TokenRefreshResponse } from "./types";

/** 이메일/비밀번호 로그인 */
export function loginRequest(email: string, password: string) {
	return unwrap<LoginResponse>(http.post("/auth/login", { email, password }));
}

/** refresh 쿠키로 access token 재발급 (구글 콜백 및 후속 silent refresh 용) */
export function refreshRequest() {
	return unwrap<TokenRefreshResponse>(http.post("/auth/token/refresh"));
}

/** 로그아웃 — 서버가 refresh 쿠키 삭제. 응답은 noContent(data=null)라 unwrap 미사용 */
export async function logoutRequest() {
	await http.post("/auth/logout");
}
