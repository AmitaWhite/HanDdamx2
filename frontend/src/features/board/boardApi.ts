import {
	ApiError,
	ensureFreshAccessToken,
	http,
	unwrap,
	unwrapVoid,
} from "@/lib/api";

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

export interface GetMyBoardPostsParams {
	type?: BoardPostType;
	status?: BoardPostStatus;
	page?: number;
	size?: number;
}

/**
 * 마이페이지 — 내가 작성한 유료 게시판 글 목록(크리에이터 구분 없이 전체).
 * 백엔드: GET /api/members/me/board-posts
 * 권한: 본인(JWT 인증된 회원)만.
 */
export function getMyBoardPosts({
	type,
	status,
	page = 0,
	size = 20,
}: GetMyBoardPostsParams = {}) {
	return unwrap<PageResponse<BoardPostResponse>>(
		http.get("/members/me/board-posts", {
			params: { type, status, page, size, sort: "createdAt,desc" },
		}),
	);
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
export async function createPremiumBoardPost({
	creatorId,
	title,
	type,
	content,
	images = [],
}: CreatePremiumBoardPostParams) {
	// multipart 는 401 재시도가 불안정할 수 있어 만료 임박 시 미리 갱신
	await ensureFreshAccessToken();

	const formData = new FormData();
	formData.append("title", title);
	formData.append("type", type);
	formData.append("content", content);
	for (const file of images) {
		formData.append("images", file, file.name);
	}

	const created = await unwrap<BoardPostResponse>(
		http.post(`/creators/${creatorId}/premium-board/posts`, formData),
	);

	// 파일을 보냈는데 서버가 이미지를 안 저장하면 성공으로 넘어가지 않게 한다
	if (images.length > 0 && created.images.length === 0) {
		throw new ApiError(
			"IMAGE_UPLOAD_EMPTY",
			`이미지가 저장되지 않았습니다. (선택한 파일 ${images.length}장). 다시 시도해 주세요.`,
		);
	}

	return created;
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

/**
 * 기존 유료 게시글에 이미지 추가 (LDJ-007).
 * 백엔드: POST /api/premium-board/posts/{postId}/images (multipart)
 * 권한: 작성자, 공식 답변 전(WAITING)만.
 * @returns 이번에 추가된 이미지 목록
 */
export async function addPremiumBoardPostImages(
	postId: number,
	images: File[],
) {
	await ensureFreshAccessToken();

	const formData = new FormData();
	for (const file of images) {
		formData.append("images", file, file.name);
	}

	return unwrap<BoardPostImageResponse[]>(
		http.post(`/premium-board/posts/${postId}/images`, formData),
	);
}

/**
 * 유료 게시글 이미지 삭제 (LDJ-008).
 * 백엔드: DELETE /api/premium-board/posts/{postId}/images/{imageId}
 * 권한: 작성자, 공식 답변 전(WAITING)만.
 */
export async function deletePremiumBoardPostImage(
	postId: number,
	imageId: number,
) {
	await unwrapVoid(
		http.delete(`/premium-board/posts/${postId}/images/${imageId}`),
	);
}

/** 백엔드 BoardCommentResponse */
export interface BoardCommentResponse {
	id: number;
	boardPostId: number;
	memberId: number;
	parentCommentId: number | null;
	depth: number;
	content: string;
	deleted: boolean;
	createdAt: string;
	updatedAt: string;
	deletedAt: string | null;
	replies: BoardCommentResponse[];
}

/**
 * 게시글 댓글·대댓글 목록 조회.
 * 백엔드: GET /api/premium-board/posts/{postId}/comments
 * 권한: 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자.
 */
export function getBoardComments(postId: number) {
	return unwrap<BoardCommentResponse[]>(
		http.get(`/premium-board/posts/${postId}/comments`),
	);
}

/**
 * 댓글 작성.
 * 백엔드: POST /api/premium-board/posts/{postId}/comments
 * 권한: 게시판 크리에이터 / 활성 유료 구독자.
 */
export function createBoardComment(postId: number, content: string) {
	return unwrap<BoardCommentResponse>(
		http.post(`/premium-board/posts/${postId}/comments`, { content }),
	);
}
