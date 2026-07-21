import { http, ensureFreshAccessToken, unwrap } from "@/lib/api";
import type { SliceResponse } from "@/lib/types";
import type {
	AttachmentResponse,
	FeedDetailResponse,
	FeedIdResponse,
	FeedSummaryResponse,
	Visibility,
} from "./types";

export interface CreateFeedParams {
	projectId: number;
	title: string;
	content: string;
	visibility: Visibility;
}

/**
 * 게시물(피드) 작성 (LYJ-001).
 * 백엔드: POST /api/feeds
 * 권한: 프로젝트 소유 크리에이터
 */
export function createFeed(params: CreateFeedParams) {
	return unwrap<FeedIdResponse>(http.post("/feeds", params));
}

/**
 * 피드에 첨부파일 추가 (LYJ-012).
 * 백엔드: POST /api/feeds/{feedId}/attachments (multipart/form-data)
 * 권한: 프로젝트 소유 크리에이터
 */
export async function addFeedAttachment(feedId: number, file: File) {
	// multipart 는 401 재시도가 불안정할 수 있어 만료 임박 시 미리 갱신
	await ensureFreshAccessToken();

	const formData = new FormData();
	formData.append("file", file, file.name);

	return unwrap<AttachmentResponse>(
		http.post(`/feeds/${feedId}/attachments`, formData),
	);
}

/**
 * 피드 상세 조회 (LYJ-002). locked===true 면 content=null.
 * 백엔드: GET /api/feeds/{feedId}
 */
export function getFeed(feedId: number | string) {
	return unwrap<FeedDetailResponse>(http.get(`/feeds/${feedId}`));
}

/**
 * 내가 작성한 피드 목록 (LYJ-010).
 * 백엔드: GET /api/feeds/me
 * 권한: 크리에이터 본인
 */
export function getMyFeeds(page = 0) {
	return unwrap<SliceResponse<FeedSummaryResponse>>(
		http.get("/feeds/me", { params: { page } }),
	);
}
