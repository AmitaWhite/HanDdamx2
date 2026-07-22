import type { ProjectCategorySummary, ProjectCreatorSummary } from "@/features/project/types";

/** 백엔드 Visibility (com.white.handdam.feed.entity.Visibility) 와 1:1 */
export type Visibility = "PUBLIC" | "FREE_SUBSCRIBER" | "PAID_SUBSCRIBER";

/** POST /api/feeds 응답 (FeedIdResponse) */
export interface FeedIdResponse {
	feedId: number;
}

/** 백엔드 FeedSummaryResponse (com.white.handdam.feed.dto.response.FeedSummaryResponse) */
export interface FeedSummaryResponse {
	id: number;
	projectId: number;
	title: string;
	content: string;
	visibility: Visibility;
	likeCount: number;
	commentCount: number;
	createdAt: string;
	updatedAt: string;
	creator: ProjectCreatorSummary;
	category: ProjectCategorySummary;
}

/**
 * 백엔드 FeedDetailResponse (com.white.handdam.feed.dto.response.FeedDetailResponse).
 * locked === true 면 content 는 null 이고, requiredLevel 에 필요한 구독 등급이 담긴다.
 */
export interface FeedDetailResponse
	extends Omit<FeedSummaryResponse, "content"> {
	content: string | null;
	locked: boolean;
	requiredLevel: string | null;
	/** 이 피드에 연결된 투표 id. 투표가 없으면 null */
	pollId: number | null;
	/** 현재 로그인한 사용자가 이미 좋아요했는지 */
	liked: boolean;
}

/** 백엔드 AttachmentResponse (com.white.handdam.feed.dto.response.AttachmentResponse) */
export interface AttachmentResponse {
	id: number;
	feedId: number;
	type: "IMAGE" | "FILE" | "VIDEO_LINK";
	url: string;
	originalName: string | null;
	fileSize: number | null;
	mimeType: string | null;
	orderIndex: number;
	createdAt: string;
}
