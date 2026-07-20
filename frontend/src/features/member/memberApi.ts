import { ensureFreshAccessToken, http, unwrap, unwrapVoid } from "@/lib/api";
import type { MemberProfileResponse, MemberSummaryResponse } from "./types";


// KSY-014: 내 프로필 조회 */
export function getMyProfile() {
	return unwrap<MemberProfileResponse>(http.get("/members/me"));
}

// KSY-016: 닉네임 변경
export function updateMyProfile(nickname: string) {
	return unwrap<MemberProfileResponse>(http.patch("/members/me", { nickname }));
}

// KSY-017: 프로필 이미지 변경
export async function updateProfileImage(image: File) {
	await ensureFreshAccessToken();

	const formData = new FormData();
	formData.append("image", image, image.name);

	return unwrap<MemberProfileResponse>(
		http.patch("/members/me/profile-image", formData),
	);
}

// KSY-018: 프로필 이미지 제거
export function deleteProfileImage() {
	return unwrap<MemberProfileResponse>(http.delete("/members/me/profile-image"));
}

// KSY-015: 내 활동 요약 (마이페이지 프로필 카드)
export function getMySummary() {
	return unwrap<MemberSummaryResponse>(http.get("/members/me/summary"));
}

/**
 * 비밀번호 변경.
 * 백엔드: PATCH /members/me/password
 * OAuth 가입자(oauthProvider !== "NONE")가 호출하면 OAUTH_MEMBER_CANNOT_CHANGE_PASSWORD 에러.
 */
export async function changePassword(currentPassword: string, newPassword: string) {
	await unwrapVoid(
		http.patch("/members/me/password", { currentPassword, newPassword }),
	);
}
