import { http, ensureFreshAccessToken, unwrap, unwrapVoid } from "@/lib/api";
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

/**
 * 최근 전체공개(PUBLIC) 피드 목록 — 비로그인 접근 가능 (LYJ-006).
 * 백엔드: GET /api/feeds/public
 */
export function getPublicFeeds(page = 0) {
	return unwrap<SliceResponse<FeedSummaryResponse>>(
		http.get("/feeds/public", { params: { page } }),
	);
}

/**
 * 회원 홈 피드 = 구독 중인 작가의 글 + 카테고리 필터 (LYJ-007).
 * 백엔드: GET /api/feeds/home
 * 권한: 로그인 필요
 */
export function getHomeFeed(categoryId?: number, page = 0) {
	return unwrap<SliceResponse<FeedSummaryResponse>>(
		http.get("/feeds/home", { params: { categoryId, page } }),
	);
}

/**
 * 전체 탐색 피드 = 전체 크리에이터의 PUBLIC 글 + 카테고리 필터, 비로그인 접근 가능 (LYJ-008).
 * 백엔드: GET /api/feeds/explore
 */
export function getExploreFeeds(categoryId?: number, page = 0) {
	return unwrap<SliceResponse<FeedSummaryResponse>>(
		http.get("/feeds/explore", { params: { categoryId, page } }),
	);
}

/**
 * 특정 크리에이터의 피드 목록 — 구독 등급에 따라 공개범위 다르게 적용 (LYJ-009).
 * 백엔드: GET /api/feeds/creators/{creatorId}
 */
export function getCreatorFeeds(creatorId: number, page = 0) {
	return unwrap<SliceResponse<FeedSummaryResponse>>(
		http.get(`/feeds/creators/${creatorId}`, { params: { page } }),
	);
}

export interface UpdateFeedParams {
	title: string;
	content: string;
	visibility: Visibility;
}

/**
 * 피드 제목·본문·공개범위 수정 (LYJ-003).
 * 백엔드: PATCH /api/feeds/{feedId}
 * 권한: 프로젝트 소유 크리에이터
 */
export function updateFeed(feedId: number | string, params: UpdateFeedParams) {
	return unwrap<FeedIdResponse>(http.patch(`/feeds/${feedId}`, params));
}

/**
 * 피드를 다른 프로젝트로 이동 (LYJ-004).
 * 백엔드: PATCH /api/feeds/{feedId}/project
 * 권한: 이동 전·후 프로젝트 모두 본인 소유여야 함
 */
export function moveFeedProject(feedId: number | string, projectId: number) {
	return unwrap<FeedIdResponse>(
		http.patch(`/feeds/${feedId}/project`, { projectId }),
	);
}

/**
 * 피드 소프트 삭제 (LYJ-005).
 * 백엔드: DELETE /api/feeds/{feedId}
 * 권한: 프로젝트 소유 크리에이터
 */
export async function deleteFeed(feedId: number | string) {
	await unwrapVoid(http.delete(`/feeds/${feedId}`));
}
