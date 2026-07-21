import { useState, type Dispatch, type SetStateAction } from "react";
import { useSubmitState } from "@/features/auth/useSubmitState";
import {
	type BoardCommentResponse,
	createBoardComment,
	createBoardCommentReply,
	deleteBoardComment,
	updateBoardComment,
} from "@/features/board/boardApi";
import {
	appendReply,
	markCommentDeleted,
	replaceCommentContent,
	upsertRootComment,
} from "./qnaCommentModel";

interface UseQnaCommentsOptions {
	postId: number;
	setComments: Dispatch<SetStateAction<BoardCommentResponse[]>>;
}

export function useQnaComments({
	postId,
	setComments,
}: UseQnaCommentsOptions) {
	const [commentDraft, setCommentDraft] = useState("");
	const [replyToId, setReplyToId] = useState<number | null>(null);
	const [replyDraft, setReplyDraft] = useState("");
	const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
	const [editingCommentDraft, setEditingCommentDraft] = useState("");

	const {
		loading: commentBusy,
		error: commentError,
		run: runComment,
	} = useSubmitState("댓글 처리에 실패했습니다. 다시 시도해 주세요.");

	function startReply(parentId: number) {
		setReplyToId(parentId);
		setReplyDraft("");
		setEditingCommentId(null);
	}

	function cancelReply() {
		setReplyToId(null);
		setReplyDraft("");
	}

	function startEditComment(commentId: number, content: string) {
		setEditingCommentId(commentId);
		setEditingCommentDraft(content);
		setReplyToId(null);
	}

	function cancelEditComment() {
		setEditingCommentId(null);
		setEditingCommentDraft("");
	}

	async function onCreateComment() {
		const content = commentDraft.trim();
		if (!content) return;
		const created = await runComment(() => createBoardComment(postId, content));
		if (created) {
			setComments((prev) => upsertRootComment(prev, created));
			setCommentDraft("");
		}
	}

	async function onCreateReply(parentId: number) {
		const content = replyDraft.trim();
		if (!content) return;
		const created = await runComment(() =>
			createBoardCommentReply(parentId, content),
		);
		if (created) {
			setComments((prev) => appendReply(prev, parentId, created));
			cancelReply();
		}
	}

	async function onUpdateComment(commentId: number) {
		const content = editingCommentDraft.trim();
		if (!content) return;
		const updated = await runComment(() =>
			updateBoardComment(commentId, content),
		);
		if (updated) {
			setComments((prev) =>
				replaceCommentContent(prev, commentId, updated.content),
			);
			cancelEditComment();
		}
	}

	async function onDeleteComment(commentId: number) {
		if (!window.confirm("이 댓글을 삭제할까요?")) return;
		const ok = await runComment(async () => {
			await deleteBoardComment(commentId);
			return true;
		});
		if (ok) {
			setComments((prev) => markCommentDeleted(prev, commentId));
			if (editingCommentId === commentId) cancelEditComment();
			if (replyToId === commentId) cancelReply();
		}
	}

	return {
		commentDraft,
		setCommentDraft,
		replyToId,
		replyDraft,
		setReplyDraft,
		editingCommentId,
		editingCommentDraft,
		setEditingCommentDraft,
		commentBusy,
		commentError,
		startReply,
		cancelReply,
		startEditComment,
		cancelEditComment,
		onCreateComment,
		onCreateReply,
		onUpdateComment,
		onDeleteComment,
	};
}
