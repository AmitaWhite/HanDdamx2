import type { BoardPostType } from "@/features/board/boardApi";
import type { QnaCategory } from "@/mocks/qna";

export const TYPE_TO_CATEGORY: Record<BoardPostType, QnaCategory> = {
	QUESTION: "제작 질문",
	FEEDBACK: "작품 피드백",
	CONTENT_SUGGESTION: "콘텐츠 제안",
	MATERIAL: "재료 추천",
	GENERAL: "일반 소통",
};

export const CATEGORY_OPTIONS: { label: QnaCategory; type: BoardPostType }[] = [
	{ label: "제작 질문", type: "QUESTION" },
	{ label: "작품 피드백", type: "FEEDBACK" },
	{ label: "콘텐츠 제안", type: "CONTENT_SUGGESTION" },
	{ label: "재료 추천", type: "MATERIAL" },
	{ label: "일반 소통", type: "GENERAL" },
];
