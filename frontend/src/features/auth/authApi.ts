import { http, unwrap, unwrapVoid } from "@/lib/api";
import type {
	AvailabilityResponse,
	LoginResponse,
	SignupResponse,
	TokenRefreshResponse,
} from "./types";

/** 이메일/비밀번호 로그인 */
export function loginRequest(email: string, password: string) {
	return unwrap<LoginResponse>(http.post("/auth/login", { email, password }));
}

/** refresh 쿠키로 access token 재발급 (구글 콜백 및 후속 silent refresh 용) */
export function refreshRequest() {
	return unwrap<TokenRefreshResponse>(http.post("/auth/token/refresh"));
}

/** 로그아웃 — 서버가 refresh 쿠키 삭제. 응답은 noContent(data=null)라 unwrapVoid 사용 */
export async function logoutRequest() {
	await unwrapVoid(http.post("/auth/logout"));
}

/** 회원가입 — 성공 시 서버가 인증 메일을 자동 발송한다(별도 발송 호출 불필요) */
export function signupRequest(
	email: string,
	password: string,
	nickname: string,
) {
	return unwrap<SignupResponse>(
		http.post("/auth/signup", { email, password, nickname }),
	);
}

/** 이메일 중복 확인 */
export function checkEmailAvailability(email: string) {
	return unwrap<AvailabilityResponse>(
		http.get("/auth/email-availability", { params: { email } }),
	);
}

/** 닉네임 중복 확인 */
export function checkNicknameAvailability(nickname: string) {
	return unwrap<AvailabilityResponse>(
		http.get("/auth/nickname-availability", { params: { nickname } }),
	);
}

/** 이메일 인증 확정 (메일 링크의 token). noContent 응답이라 unwrapVoid 사용 */
export async function confirmEmailVerification(token: string) {
	await unwrapVoid(http.post("/auth/email-verifications/confirm", { token }));
}

/** 인증 메일 재발송. noContent 응답이라 unwrapVoid 사용 */
export async function resendVerificationEmail(email: string) {
	await unwrapVoid(http.post("/auth/email-verifications/resend", { email }));
}
