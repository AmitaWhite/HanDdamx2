import { http, unwrap } from "@/lib/api";

/** 백엔드 CreatorProfileResponse (com.white.handdam.creator.dto.response.CreatorProfileResponse) */
export interface CreatorProfileResponse {
	creatorId: number;
	memberId: number;
	nickname: string;
	profileImageUrl: string | null;
	coverImageUrl: string | null;
	representativeImageUrl: string | null;
	introduction: string | null;
	benefitsDescription: string | null;
	subscriptionPrice: number;
	subscriptionLevel: string | null;
	subscriptionStatus: string | null;
	subscriberCount: number;
	projectCount: number;
	feedCount: number;
	isMine: boolean;
}

/**
 * 크리에이터 공개 프로필 조회.
 * 백엔드: GET /api/creators/{creatorId}
 */
export function getCreatorProfile(creatorId: number) {
	return unwrap<CreatorProfileResponse>(http.get(`/creators/${creatorId}`));
}
