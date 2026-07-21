import { useEffect, useState, type Dispatch, type FormEvent, type SetStateAction } from "react";
import { useSubmitState } from "@/features/auth/useSubmitState";
import {
	type BoardAnswerResponse,
	type BoardPostResponse,
	createBoardAnswer,
	deleteBoardAnswer,
	updateBoardAnswer,
} from "@/features/board/boardApi";

interface UseQnaAnswerOptions {
	postId: number;
	setPost: Dispatch<SetStateAction<BoardPostResponse | null>>;
	answer: BoardAnswerResponse | null;
	setAnswer: Dispatch<SetStateAction<BoardAnswerResponse | null>>;
}

export function useQnaAnswer({
	postId,
	setPost,
	answer,
	setAnswer,
}: UseQnaAnswerOptions) {
	const [answerDraft, setAnswerDraft] = useState("");
	const [answerEditing, setAnswerEditing] = useState(false);

	const {
		loading: answerBusy,
		error: answerError,
		run: runAnswer,
	} = useSubmitState("공식 답변 처리에 실패했습니다. 다시 시도해 주세요.");

	useEffect(() => {
		if (!answerEditing) {
			setAnswerDraft(answer?.content ?? "");
		}
	}, [answer, answerEditing]);

	function startAnswerEdit() {
		if (!answer) return;
		setAnswerDraft(answer.content);
		setAnswerEditing(true);
	}

	function cancelAnswerEdit() {
		if (answer) setAnswerDraft(answer.content);
		setAnswerEditing(false);
	}

	async function onCreateAnswer(e: FormEvent) {
		e.preventDefault();
		const content = answerDraft.trim();
		if (!content) return;

		const created = await runAnswer(() => createBoardAnswer(postId, content));
		if (created) {
			setAnswer(created);
			setAnswerDraft(created.content);
			setPost((prev) => (prev ? { ...prev, status: "ANSWERED" } : prev));
		}
	}

	async function onUpdateAnswer(e: FormEvent) {
		e.preventDefault();
		if (!answer) return;
		const content = answerDraft.trim();
		if (!content) return;

		const updated = await runAnswer(() =>
			updateBoardAnswer(answer.id, content),
		);
		if (updated) {
			setAnswer(updated);
			setAnswerDraft(updated.content);
			setAnswerEditing(false);
		}
	}

	async function onDeleteAnswer() {
		if (!answer) return;
		if (
			!window.confirm(
				"공식 답변을 삭제할까요? 게시글이 답변 대기로 돌아갑니다.",
			)
		) {
			return;
		}
		const ok = await runAnswer(async () => {
			await deleteBoardAnswer(answer.id);
			return true;
		});
		if (ok) {
			setAnswer(null);
			setAnswerDraft("");
			setAnswerEditing(false);
			setPost((prev) => (prev ? { ...prev, status: "WAITING" } : prev));
		}
	}

	return {
		answerDraft,
		setAnswerDraft,
		answerEditing,
		answerBusy,
		answerError,
		startAnswerEdit,
		cancelAnswerEdit,
		onCreateAnswer,
		onUpdateAnswer,
		onDeleteAnswer,
	};
}
