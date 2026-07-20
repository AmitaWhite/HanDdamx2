import { http, unwrap, unwrapVoid } from "@/lib/api";

/**
 * 백엔드 BoardPostType (com.white.handdam.board.entity.BoardPostType)
 */
export type BoardPostType =
	| "QUESTION"
	| "FEEDBACK"
	| "CONTENT_SUGGESTION"
	| "MATERIAL"
	| "GENERAL";

/**
 * 백엔드 BoardPostStatus (com.white.handdam.board.entity.BoardPostStatus)
 */
export type BoardPostStatus = "WAITING" | "ANSWERED";

/** 백엔드 BoardPostImageResponse */
export interface BoardPostImageResponse {
	id: number;
	url: string;
	storageKey?: string;
	originalName?: string;
	fileSize?: number;
	mimeType?: string;
	orderIndex: number;
	createdAt?: string;
}

export interface BoardPostResponse {
	id: number;
	creatorId: number;
	memberId: number;
	title: string;
	type: BoardPostType;
	content: string;
	status: BoardPostStatus;
	createdAt: string;
	updatedAt: string;
	deletedAt: string | null;
	images: BoardPostImageResponse[];
}

/**
 * Spring Data Page 응답 — 필요한 필드만 노출.
 * (호출부는 content/hasNext/pageNumber 위주로 소비)
 */
export interface PageResponse<T> {
	content: T[];
	number: number;
	size: number;
	totalElements: number;
	totalPages: number;
	first: boolean;
	last: boolean;
	empty: boolean;
}

export interface GetPremiumBoardPostsParams {
	creatorId: number;
	type?: BoardPostType;
	status?: BoardPostStatus;
	page?: number;
	size?: number;
}

/**
 * 크리에이터별 유료 게시판 게시글 목록 조회 (LDJ-001).
 * 백엔드: GET /api/creators/{creatorId}/premium-board/posts
 * 권한: 게시판 크리에이터 본인 또는 활성 유료 구독자.
 */
export function getPremiumBoardPosts({
	creatorId,
	type,
	status,
	page = 0,
	size = 20,
}: GetPremiumBoardPostsParams) {
	return unwrap<PageResponse<BoardPostResponse>>(
		http.get(`/creators/${creatorId}/premium-board/posts`, {
			params: {
				type,
				status,
				page,
				size,
				sort: "createdAt,desc",
			},
		}),
	);
}

export interface CreatePremiumBoardPostParams {
	creatorId: number;
	title: string;
	type: BoardPostType;
	content: string;
	/** 선택. 백엔드 @RequestPart("images") — multipart */
	images?: File[];
}

/**
 * 유료 게시판 게시글·이미지 작성 (LDJ-002).
 * 백엔드: POST /api/creators/{creatorId}/premium-board/posts (multipart/form-data)
 * 권한: 게시판 크리에이터 본인 또는 활성 유료 구독자.
 */
export function createPremiumBoardPost({
	creatorId,
	title,
	type,
	content,
	images = [],
}: CreatePremiumBoardPostParams) {
	const formData = new FormData();
	formData.append("title", title);
	formData.append("type", type);
	formData.append("content", content);
	for (const file of images) {
		formData.append("images", file);
	}

	return unwrap<BoardPostResponse>(
		http.post(`/creators/${creatorId}/premium-board/posts`, formData, {
			// 기본 JSON Content-Type 을 덮어쓰고, boundary 는 axios/브라우저가 붙인다.
			headers: { "Content-Type": "multipart/form-data" },
		}),
	);
}

/**
 * 유료 게시글 상세 조회 (LDJ-003).
 * 백엔드: GET /api/premium-board/posts/{postId}
 */
export function getPremiumBoardPost(postId: number) {
	return unwrap<BoardPostResponse>(http.get(`/premium-board/posts/${postId}`));
}

export interface UpdatePremiumBoardPostParams {
	postId: number;
	title: string;
	type: BoardPostType;
	content: string;
}

/**
 * 유료 게시글 수정 — 공식 답변 전만 (LDJ-004).
 * 백엔드: PATCH /api/premium-board/posts/{postId}
 * 권한: 작성자
 */
export function updatePremiumBoardPost({
	postId,
	title,
	type,
	content,
}: UpdatePremiumBoardPostParams) {
	return unwrap<BoardPostResponse>(
		http.patch(`/premium-board/posts/${postId}`, { title, type, content }),
	);
}

/**
 * 유료 게시글 소프트 삭제 (LDJ-005).
 * 백엔드: DELETE /api/premium-board/posts/{postId}
 * 권한: 작성자
 */
export async function deletePremiumBoardPost(postId: number) {
	await unwrapVoid(http.delete(`/premium-board/posts/${postId}`));
}
