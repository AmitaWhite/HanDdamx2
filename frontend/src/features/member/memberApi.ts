import { http, unwrap } from "@/lib/api";

/**
 * 백엔드 MemberProfileResponse (com.white.handdam.member.dto.response.MemberProfileResponse)
 * 표시용 필드만 정의 — role/memberId는 JWT로 이미 확보하므로 여기선 닉네임 보강 목적으로만 쓴다.
 */
export interface MemberProfileResponse {
	id: number;
	email: string;
	nickname: string;
	profileImageUrl: string | null;
}

/**
 * 내 프로필 조회.
 * 백엔드: GET /api/members/me
 */
export function getMyProfile() {
	return unwrap<MemberProfileResponse>(http.get("/members/me"));
}
