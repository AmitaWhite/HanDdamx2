import { http, unwrap } from "@/lib/api";
import type { FeedSummaryResponse } from "@/features/feed/types";
import type { SliceResponse } from "@/lib/types";
import type { ProjectResponse } from "./types";

/**
 * 내 프로젝트 관리 목록 조회.
 * 백엔드: GET /api/creators/me/projects
 * 권한: 크리에이터 본인
 */
export function getMyProjects() {
	return unwrap<ProjectResponse[]>(http.get("/creators/me/projects"));
}

/**
 * 프로젝트별 피드 목록 — 구독 등급에 따라 공개범위 다르게 적용 (LYJ-011).
 * 백엔드: GET /api/projects/{projectId}/feeds
 * 권한: 전체 (비로그인 포함)
 */
export function getProjectFeeds(projectId: number, page = 0) {
	return unwrap<SliceResponse<FeedSummaryResponse>>(
		http.get(`/projects/${projectId}/feeds`, { params: { page } }),
	);
}
