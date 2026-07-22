import { http, unwrap } from "@/lib/api";
import type { CreatorProfile, ProjectSummary } from "./types";

/** GET /api/creators/{creatorId} — 크리에이터 공개 프로필 조회 */
export function getCreatorProfile(creatorId: number) {
  return unwrap<CreatorProfile>(http.get(`/creators/${creatorId}`));
}

/** GET /api/creators/{creatorId}/projects — 크리에이터 공개 프로젝트 목록 */
export function getCreatorProjects(creatorId: number): Promise<ProjectSummary[]> {
  return unwrap(http.get<{ success: boolean; data: ProjectSummary[]; error: null }>(`/creators/${creatorId}/projects`));
}

/** GET /api/projects/{projectId} — 프로젝트 상세 */
export function getProject(projectId: number): Promise<ProjectSummary> {
  return unwrap(http.get(`/projects/${projectId}`));
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
  return unwrap(http.delete(`/projects/${projectId}`));
}
