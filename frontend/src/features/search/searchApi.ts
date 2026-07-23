import { http, unwrap } from "@/lib/api";
import type { SliceResponse } from "@/lib/types";
import type { FeedSummaryResponse } from "@/features/feed/types";
import type { CreatorSearchResponse } from "./types";

/**
 * 작품(피드) 검색 — 제목/본문 keyword + 카테고리 필터, 무한스크롤.
 * 백엔드: GET /api/search/feeds
 */
export function searchFeeds(keyword: string, categoryId?: number, page = 0) {
	return unwrap<SliceResponse<FeedSummaryResponse>>(
		http.get("/search/feeds", { params: { keyword, categoryId, page } }),
	);
}

/**
 * 크리에이터(작가명) 검색.
 * 백엔드: GET /api/search/creators
 */
export function searchCreators(keyword: string, page = 0) {
	return unwrap<SliceResponse<CreatorSearchResponse>>(
		http.get("/search/creators", { params: { keyword, page } }),
	);
}
