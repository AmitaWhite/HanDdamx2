import { http, unwrap } from "@/lib/api";
import type { CreatorProfile, ProjectSummary } from "./types";

/**
 * 크리에이터 공개 프로필 조회.
 * 백엔드: GET /api/creators/{creatorId}
 */
export function getCreatorProfile(creatorId: number) {
  return unwrap<CreatorProfile>(http.get(`/creators/${creatorId}`));
}

/** GET /api/creators/{creatorId}/projects */
export function getCreatorProjects(creatorId: number): Promise<ProjectSummary[]> {
  return unwrap(http.get<{ success: boolean; data: ProjectSummary[]; error: null }>(`/creators/${creatorId}/projects`));
}
