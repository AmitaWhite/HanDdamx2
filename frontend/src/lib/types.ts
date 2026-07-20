/**
 * 백엔드 공통 응답 계약 (com.white.handdam.global.response.ApiResponse)
 *   { success: boolean, data: T | null, error: ErrorResponse | null }
 */
export interface ErrorResponse {
	code: string;
	message: string;
	// 백엔드 ErrorResponse 필드가 확정되면 여기에 맞춰 확장
	[key: string]: unknown;
}

export interface ApiResponse<T> {
	success: boolean;
	data: T | null;
	error: ErrorResponse | null;
}

/** 페이지네이션(Slice) 응답 — com.white.handdam.global.response.SliceResponse 대응 */
export interface SliceResponse<T> {
	content: T[];
	hasNext: boolean;
	page: number;
	size: number;
}
