/** 백엔드 FeedCommentResponse (com.white.handdam.comment.dto.response.FeedCommentResponse) */
export interface FeedCommentResponse {
	id: number;
	memberId: number;
	nickname: string;
	profileImageUrl: string | null;
	content: string;
	/** 0 = 최상위 댓글, 1 = 답글 (답글의 답글은 없음) */
	depth: number;
	createdAt: string;
	updatedAt: string;
	replies: FeedCommentResponse[];
}

/** POST/PATCH 댓글 응답 (FeedCommentIdResponse) */
export interface FeedCommentIdResponse {
	commentId: number;
}
