import { http, unwrap, unwrapVoid } from "@/lib/api";
import type { CreatorProfile, FeedSummary, ProjectSummary, SliceResponse } from "./types";

/** GET /api/creators/{creatorId} — 크리에이터 공개 프로필 조회 */
export function getCreatorProfile(creatorId: number) {
  return unwrap<CreatorProfile>(http.get(`/creators/${creatorId}`));
}

/** GET /api/creators/me — 내 크리에이터 프로필 조회 */
export function getMyCreatorProfile(): Promise<CreatorProfile> {
  return unwrap(http.get("/creators/me"));
}

/** PATCH /api/creators/me — 소개글·구독 혜택 설명 수정 */
export function updateCreatorProfile(
  introduction: string | null,
  benefitsDescription: string | null,
): Promise<CreatorProfile> {
  return unwrap(http.patch("/creators/me", { introduction, benefitsDescription }));
}

/** PATCH /api/creators/me/subscription-price — 월 구독료 변경 */
export function updateSubscriptionPrice(subscriptionPrice: number): Promise<CreatorProfile> {
  return unwrap(http.patch("/creators/me/subscription-price", { subscriptionPrice }));
}

/** GET /api/creators/{creatorId}/projects — 크리에이터 공개 프로젝트 목록 */
export function getCreatorProjects(creatorId: number): Promise<ProjectSummary[]> {
  return unwrap(http.get<{ success: boolean; data: ProjectSummary[]; error: null }>(`/creators/${creatorId}/projects`));
}

/** GET /api/creators/me/projects — 내 프로젝트 목록 (크리에이터 전용) */
export function getMyProjects(): Promise<ProjectSummary[]> {
  return unwrap(http.get<{ success: boolean; data: ProjectSummary[]; error: null }>("/creators/me/projects"));
}

/** GET /api/projects/{projectId} — 프로젝트 상세 */
export function getProject(projectId: number): Promise<ProjectSummary> {
  return unwrap(http.get(`/projects/${projectId}`));
}

/**
 * GET /api/projects/{projectId}/feeds — 프로젝트 피드 목록
 * 백엔드가 SliceResponse를 ApiResponse 없이 직접 반환하므로 unwrap 불가, r.data 직접 사용
 */
export function getProjectFeeds(
  projectId: number,
  page = 0,
  size = 20,
): Promise<SliceResponse<FeedSummary>> {
  return http
    .get<SliceResponse<FeedSummary>>(`/projects/${projectId}/feeds`, { params: { page, size } })
    .then((r) => r.data);
}

/** POST /api/projects — 프로젝트 생성 (multipart/form-data) */
export function createProject(
  title: string,
  categoryId: number,
  description?: string | null,
  coverImage?: File | null,
): Promise<ProjectSummary> {
  const form = new FormData();
  form.append(
    "metadata",
    new Blob([JSON.stringify({ title, categoryId, description })], { type: "application/json" }),
  );
  if (coverImage) form.append("coverImage", coverImage);
  return unwrap(http.post("/projects", form));
}

/** PATCH /api/projects/{projectId} — 프로젝트 수정 (multipart/form-data) */
export function updateProject(
  projectId: number,
  title: string,
  categoryId: number,
  description?: string | null,
  removeCoverImage = false,
  coverImage?: File | null,
): Promise<ProjectSummary> {
  const form = new FormData();
  form.append(
    "metadata",
    new Blob(
      [JSON.stringify({ title, categoryId, description, removeCoverImage })],
      { type: "application/json" },
    ),
  );
  if (coverImage) form.append("coverImage", coverImage);
  return unwrap(http.patch(`/projects/${projectId}`, form));
}

/** DELETE /api/projects/{projectId} — 빈 프로젝트 삭제 */
export function deleteProject(projectId: number): Promise<void> {
  return unwrapVoid(http.delete(`/projects/${projectId}`));
}
