import { http, unwrap } from "@/lib/api";

export interface CategorySummary {
  categoryId: number;
  name: string;
  active: boolean;
}

/** GET /api/categories — 활성 카테고리 목록 (비로그인 포함) */
export function getCategories(): Promise<CategorySummary[]> {
  return unwrap(http.get("/categories"));
}
