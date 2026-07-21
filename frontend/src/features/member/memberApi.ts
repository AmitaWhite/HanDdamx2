import { ensureFreshAccessToken, http, unwrap, unwrapVoid } from "@/lib/api";
import type { SliceResponse } from "@/lib/types";
import type {MemberProfileResponse, MemberSummaryResponse,
	MyBoardCommentResponse, MyFeedCommentResponse,
} from "./types";


// KSY-014: 내 프로필 조회
export function getMyProfile() {
	return unwrap<MemberProfileResponse>(http.get("/members/me"));
}

/** 백엔드 MemberPublicProfileResponse */
export interface MemberPublicProfileResponse {
	id: number;
	nickname: string;
	profileImageUrl: string | null;
	isCreator: boolean;
	createdAt: string;
}

/**
 * 회원 공개 프로필 조회 (닉네임·프로필 이미지).
 * 백엔드: GET /api/members/{memberId}
 */
export function getMemberPublicProfile(memberId: number) {
	return unwrap<MemberPublicProfileResponse>(
		http.get(`/members/${memberId}`),
	);
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

// KSY-011: 비밀번호 변경
export async function changePassword(currentPassword: string, newPassword: string) {
	await unwrapVoid(
		http.patch("/members/me/password", { currentPassword, newPassword }),
	);
}

// KSY-020: 작성한 피드 댓글 목록
export function getMyFeedComments(page = 0, size = 3) {
	return unwrap<SliceResponse<MyFeedCommentResponse>>(
		http.get("/members/me/feed-comments", { params: { page, size } }),
	);
}

// KSY-021: 작성한 게시판 댓글 목록
export function getMyBoardComments(page = 0, size = 3) {
	return unwrap<SliceResponse<MyBoardCommentResponse>>(
		http.get("/members/me/board-comments", { params: { page, size } }),
	);
}
