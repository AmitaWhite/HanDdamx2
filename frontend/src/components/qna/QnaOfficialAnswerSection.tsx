import type { FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import type { BoardAnswerResponse } from "@/features/board/boardApi";
import { formatRelativeTime } from "@/lib/relativeTime";

interface QnaOfficialAnswerSectionProps {
	answer: BoardAnswerResponse;
	answerDraft: string;
	answerEditing: boolean;
	answerBusy: boolean;
	canManageAnswer: boolean;
	onDraftChange: (value: string) => void;
	onStartEdit: () => void;
	onCancelEdit: () => void;
	onUpdate: (e: FormEvent) => void;
	onDelete: () => void;
}

export function QnaOfficialAnswerSection({
	answer,
	answerDraft,
	answerEditing,
	answerBusy,
	canManageAnswer,
	onDraftChange,
	onStartEdit,
	onCancelEdit,
	onUpdate,
	onDelete,
}: QnaOfficialAnswerSectionProps) {
	return (
		<div className="mt-8 rounded-xl border border-primary/30 bg-primary/5 p-5">
			<div className="mb-3 flex items-center justify-between gap-3">
				<div className="flex items-center gap-2">
					<Icon name="workspace_premium" className="text-primary" />
					<span className="text-label-md font-label-md text-on-surface">
						크리에이터의 공식 답변
					</span>
				</div>
				{canManageAnswer && !answerEditing && (
					<div className="flex gap-2">
						<Button
							type="button"
							variant="secondary"
							size="sm"
							onClick={onStartEdit}
						>
							수정
						</Button>
						<Button
							type="button"
							variant="outline"
							size="sm"
							onClick={onDelete}
							disabled={answerBusy}
						>
							{answerBusy ? "삭제 중…" : "삭제"}
						</Button>
					</div>
				)}
			</div>
			{answerEditing ? (
				<form className="flex flex-col gap-3" onSubmit={onUpdate} noValidate>
					<textarea
						value={answerDraft}
						onChange={(e) => onDraftChange(e.target.value)}
						rows={5}
						required
						className="w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-4 py-3 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					/>
					<div className="flex gap-3">
						<Button
							type="button"
							variant="secondary"
							fullWidth
							onClick={onCancelEdit}
							disabled={answerBusy}
						>
							취소
						</Button>
						<Button type="submit" fullWidth disabled={answerBusy}>
							{answerBusy ? "저장 중…" : "저장"}
						</Button>
					</div>
				</form>
			) : (
				<>
					<div className="whitespace-pre-wrap text-body-md text-on-surface">
						{answer.content}
					</div>
					<p className="mt-3 text-caption font-caption text-secondary">
						{formatRelativeTime(answer.createdAt)}
					</p>
				</>
			)}
		</div>
	);
}
