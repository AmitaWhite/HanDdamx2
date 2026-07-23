import { http, unwrap } from "@/lib/api";
import type { CreatorApplication, PageResponse } from "./types";

/** POST /api/creator-applications (multipart/form-data) */
export function applyCreator(
  introduction: string,
  representativeImage?: File | null,
): Promise<CreatorApplication> {
  const form = new FormData();
  form.append("metadata", new Blob([JSON.stringify({ introduction })], { type: "application/json" }));
  if (representativeImage) {
    form.append("representativeImage", representativeImage);
  }
  return unwrap(http.post("/creator-applications", form));}

/** GET /api/creator-applications/me */
export function getMyLatestApplication(): Promise<CreatorApplication> {
  return unwrap(http.get("/creator-applications/me"));
}

/** GET /api/admin/creator-applications */
export function getAdminApplicationList(
  status?: "PENDING" | "APPROVED" | "REJECTED",
  page = 0,
  size = 10,
): Promise<PageResponse<CreatorApplication>> {
  return unwrap(
    http.get("/admin/creator-applications", {
      params: { ...(status ? { status } : {}), page, size },
    }),
  );
}

/** PATCH /api/admin/creator-applications/{id}/approve */
export function approveApplication(applicationId: number): Promise<CreatorApplication> {
  return unwrap(http.patch(`/admin/creator-applications/${applicationId}/approve`));
}

/** PATCH /api/admin/creator-applications/{id}/reject */
export function rejectApplication(
  applicationId: number,
  rejectReason: string,
): Promise<CreatorApplication> {
  return unwrap(
    http.patch(`/admin/creator-applications/${applicationId}/reject`, { rejectReason }),
  );
}
