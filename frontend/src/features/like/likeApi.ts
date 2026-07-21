import { http, unwrap } from "@/lib/api";
import type { FeedLikeResponse } from "./types";

/**
 * 피드 좋아요.
 * 백엔드: POST /api/feeds/{feedId}/likes
 */
export function likeFeed(feedId: number | string) {
	return unwrap<FeedLikeResponse>(http.post(`/feeds/${feedId}/likes`));
}

/**
 * 피드 좋아요 취소.
 * 백엔드: DELETE /api/feeds/{feedId}/likes
 */
export function unlikeFeed(feedId: number | string) {
	return unwrap<FeedLikeResponse>(http.delete(`/feeds/${feedId}/likes`));
}
