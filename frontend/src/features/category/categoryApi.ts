import { http, unwrap } from "@/lib/api";
import type { CategoryResponse } from "./types";

/**
 * 활성 카테고리 목록 조회 — 비로그인 접근 가능.
 * 백엔드: GET /api/categories
 */
export function getActiveCategories() {
	return unwrap<CategoryResponse[]>(http.get("/categories"));
}

/** @deprecated getActiveCategories 사용 — DashboardProjectPage/DashboardProjectsPage 정리 후 제거 예정 */
export type CategorySummary = CategoryResponse;

/** @deprecated getActiveCategories 사용 — DashboardProjectPage/DashboardProjectsPage 정리 후 제거 예정 */
export const getCategories = getActiveCategories;
