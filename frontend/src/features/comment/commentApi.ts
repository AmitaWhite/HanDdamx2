import { http, unwrap, unwrapVoid } from "@/lib/api";
import type { FeedCommentIdResponse, FeedCommentResponse } from "./types";

/**
 * 피드 댓글 목록 (최상위 댓글 + 답글 1단 포함).
 * 백엔드: GET /api/feeds/{feedId}/comments
 * 권한: 전체 (비로그인 포함)
 */
export function getFeedComments(feedId: number | string) {
	return unwrap<FeedCommentResponse[]>(http.get(`/feeds/${feedId}/comments`));
}

/**
 * 댓글 작성.
 * 백엔드: POST /api/feeds/{feedId}/comments
 */
export function createFeedComment(feedId: number | string, content: string) {
	return unwrap<FeedCommentIdResponse>(
		http.post(`/feeds/${feedId}/comments`, { content }),
	);
}

/**
 * 답글 작성 (1단까지만 허용).
 * 백엔드: POST /api/feeds/{feedId}/comments/{parentCommentId}/replies
 */
export function createFeedCommentReply(
	feedId: number | string,
	parentCommentId: number,
	content: string,
) {
	return unwrap<FeedCommentIdResponse>(
		http.post(`/feeds/${feedId}/comments/${parentCommentId}/replies`, { content }),
	);
}

/**
 * 댓글/답글 수정.
 * 백엔드: PATCH /api/feeds/{feedId}/comments/{commentId}
 */
export function updateFeedComment(
	feedId: number | string,
	commentId: number,
	content: string,
) {
	return unwrap<FeedCommentIdResponse>(
		http.patch(`/feeds/${feedId}/comments/${commentId}`, { content }),
	);
}

/**
 * 댓글/답글 삭제.
 * 백엔드: DELETE /api/feeds/{feedId}/comments/{commentId}
 */
export async function deleteFeedComment(
	feedId: number | string,
	commentId: number,
) {
	await unwrapVoid(http.delete(`/feeds/${feedId}/comments/${commentId}`));
}
