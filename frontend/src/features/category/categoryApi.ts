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

/** GET /api/admin/categories — 전체 카테고리 목록 (비활성 포함, 관리자 전용) */
export function getAllCategories(): Promise<CategoryResponse[]> {
  return unwrap(http.get("/admin/categories"));
}

/** POST /api/admin/categories — 카테고리 생성 */
export function createCategory(name: string): Promise<CategoryResponse> {
  return unwrap(http.post("/admin/categories", { name }));
}

/** PATCH /api/admin/categories/{id} — 카테고리 이름 수정 */
export function updateCategoryName(categoryId: number, name: string): Promise<CategoryResponse> {
  return unwrap(http.patch(`/admin/categories/${categoryId}`, { name }));
}

/** PATCH /api/admin/categories/{id}/activation?active= — 활성/비활성 토글 */
export function updateActivation(categoryId: number, active: boolean): Promise<CategoryResponse> {
  return unwrap(http.patch(`/admin/categories/${categoryId}/activation`, null, { params: { active } }));
}
