import type { BoardCommentResponse } from "@/features/board/boardApi";

export function toNumericPostId(value: string): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isInteger(n) && n > 0 ? n : null;
}

export function countComments(comments: BoardCommentResponse[]): number {
	return comments.reduce(
		(sum, c) => sum + 1 + (c.replies?.length ?? 0),
		0,
	);
}
