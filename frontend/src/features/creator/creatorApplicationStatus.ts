import type { CreatorApplicationStatus } from "./types";

// 크리에이터 전환 신청 상태 라벨
export const CREATOR_APPLICATION_STATUS_LABEL: Record<CreatorApplicationStatus, string> = {
  PENDING: "심사 중",
  APPROVED: "승인 완료",
  REJECTED: "거절됨",
};

// Material Symbols Outlined 아이콘 이름
export const CREATOR_APPLICATION_STATUS_ICON: Record<CreatorApplicationStatus, string> = {
  PENDING: "schedule",
  APPROVED: "check_circle",
  REJECTED: "cancel",
};

export const CREATOR_APPLICATION_STATUS_TEXT_COLOR: Record<CreatorApplicationStatus, string> = {
  PENDING: "text-yellow-700",
  APPROVED: "text-green-700",
  REJECTED: "text-error",
};

export const CREATOR_APPLICATION_STATUS_BG: Record<CreatorApplicationStatus, string> = {
  PENDING: "bg-yellow-50",
  APPROVED: "bg-green-50",
  REJECTED: "bg-red-50",
};