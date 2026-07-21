import type { BoardCommentResponse } from "@/features/board/boardApi";

export function upsertRootComment(
	list: BoardCommentResponse[],
	comment: BoardCommentResponse,
): BoardCommentResponse[] {
	const idx = list.findIndex((c) => c.id === comment.id);
	if (idx < 0) {
		return [...list, { ...comment, replies: comment.replies ?? [] }];
	}
	const next = [...list];
	next[idx] = {
		...comment,
		replies: comment.replies ?? next[idx].replies ?? [],
	};
	return next;
}

export function markCommentDeleted(
	list: BoardCommentResponse[],
	commentId: number,
): BoardCommentResponse[] {
	return list.map((c) => {
		if (c.id === commentId) {
			return { ...c, deleted: true, content: "삭제된 댓글입니다." };
		}
		return {
			...c,
			replies: (c.replies ?? []).map((r) =>
				r.id === commentId
					? { ...r, deleted: true, content: "삭제된 댓글입니다." }
					: r,
			),
		};
	});
}

export function appendReply(
	list: BoardCommentResponse[],
	parentId: number,
	reply: BoardCommentResponse,
): BoardCommentResponse[] {
	return list.map((c) =>
		c.id === parentId
			? { ...c, replies: [...(c.replies ?? []), reply] }
			: c,
	);
}

export function replaceCommentContent(
	list: BoardCommentResponse[],
	commentId: number,
	content: string,
): BoardCommentResponse[] {
	return list.map((c) => {
		if (c.id === commentId) {
			return { ...c, content };
		}
		return {
			...c,
			replies: (c.replies ?? []).map((r) =>
				r.id === commentId ? { ...r, content } : r,
			),
		};
	});
}
