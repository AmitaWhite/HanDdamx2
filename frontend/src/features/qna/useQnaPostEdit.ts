import type { ChangeEvent, FormEvent } from "react";
import { useState, type Dispatch, type SetStateAction } from "react";
import { useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { useSubmitState } from "@/features/auth/useSubmitState";
import {
	type BoardPostResponse,
	type BoardPostType,
	addPremiumBoardPostImages,
	deletePremiumBoardPost,
	deletePremiumBoardPostImage,
	updatePremiumBoardPost,
} from "@/features/board/boardApi";

interface UseQnaPostEditOptions {
	postId: number;
	post: BoardPostResponse | null;
	setPost: Dispatch<SetStateAction<BoardPostResponse | null>>;
}

export function useQnaPostEdit({
	postId,
	post,
	setPost,
}: UseQnaPostEditOptions) {
	const navigate = useNavigate();
	const [editing, setEditing] = useState(false);
	const [editTitle, setEditTitle] = useState("");
	const [editType, setEditType] = useState<BoardPostType>("QUESTION");
	const [editContent, setEditContent] = useState("");

	const {
		loading: saving,
		error: saveError,
		run: runSave,
	} = useSubmitState("수정에 실패했습니다. 다시 시도해 주세요.");
	const {
		loading: deleting,
		error: deleteError,
		run: runDelete,
	} = useSubmitState("삭제에 실패했습니다. 다시 시도해 주세요.");
	const {
		loading: imageBusy,
		error: imageError,
		run: runImage,
	} = useSubmitState("이미지 처리에 실패했습니다. 다시 시도해 주세요.");

	function startEdit() {
		if (!post) return;
		setEditTitle(post.title);
		setEditType(post.type);
		setEditContent(post.content);
		setEditing(true);
	}

	function cancelEdit() {
		setEditing(false);
	}

	async function onSave(e: FormEvent) {
		e.preventDefault();
		const title = editTitle.trim();
		const content = editContent.trim();
		if (!title || !content) return;

		const updated = await runSave(() =>
			updatePremiumBoardPost({
				postId,
				title,
				type: editType,
				content,
			}),
		);
		if (updated) {
			setPost(updated);
			setEditing(false);
		}
	}

	async function onDelete() {
		if (!window.confirm("이 글을 삭제할까요?")) return;
		const ok = await runDelete(async () => {
			await deletePremiumBoardPost(postId);
			return true;
		});
		if (ok && post) {
			navigate(paths.creatorQna(post.creatorId), { replace: true });
		}
	}

	async function onAddImages(e: ChangeEvent<HTMLInputElement>) {
		const selected = e.target.files;
		if (!selected || selected.length === 0) return;
		const files = Array.from(selected);
		e.target.value = "";

		const added = await runImage(() =>
			addPremiumBoardPostImages(postId, files),
		);
		if (added) {
			setPost((prev) =>
				prev ? { ...prev, images: [...prev.images, ...added] } : prev,
			);
		}
	}

	async function onRemoveImage(imageId: number) {
		if (!window.confirm("이 이미지를 삭제할까요?")) return;
		const ok = await runImage(async () => {
			await deletePremiumBoardPostImage(postId, imageId);
			return true;
		});
		if (ok) {
			setPost((prev) =>
				prev
					? {
							...prev,
							images: prev.images.filter((img) => img.id !== imageId),
						}
					: prev,
			);
		}
	}

	return {
		editing,
		editTitle,
		setEditTitle,
		editType,
		setEditType,
		editContent,
		setEditContent,
		saving,
		saveError,
		deleting,
		deleteError,
		imageBusy,
		imageError,
		startEdit,
		cancelEdit,
		onSave,
		onDelete,
		onAddImages,
		onRemoveImage,
	};
}
