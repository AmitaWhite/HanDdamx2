import { http, unwrap } from "@/lib/api";
import type { ProjectResponse } from "./types";

/**
 * 내 프로젝트 관리 목록 조회.
 * 백엔드: GET /api/creators/me/projects
 * 권한: 크리에이터 본인
 */
export function getMyProjects() {
	return unwrap<ProjectResponse[]>(http.get("/creators/me/projects"));
}
