import { http, unwrap } from "@/lib/api";
import type { CategoryResponse } from "./types";

/**
 * 활성 카테고리 목록 조회 — 비로그인 접근 가능.
 * 백엔드: GET /api/categories
 */
export function getActiveCategories() {
	return unwrap<CategoryResponse[]>(http.get("/categories"));

export interface CategorySummary {
  categoryId: number;
  name: string;
  active: boolean;
}

/** GET /api/categories — 활성 카테고리 목록 (비로그인 포함) */
export function getCategories(): Promise<CategorySummary[]> {
  return unwrap(http.get("/categories"));
}
