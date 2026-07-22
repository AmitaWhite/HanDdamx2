import type { Visibility } from "@/features/feed/types";

/** 게시물 목록 카드에 표시하는 공개범위 뱃지 라벨. */
export const VISIBILITY_BADGE_LABEL: Record<Visibility, string> = {
	PUBLIC: "전체",
	FREE_SUBSCRIBER: "무료",
	PAID_SUBSCRIBER: "유료",
};
