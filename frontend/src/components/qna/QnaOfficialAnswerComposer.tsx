import type { FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";

interface QnaOfficialAnswerComposerProps {
	answerDraft: string;
	answerBusy: boolean;
	onDraftChange: (value: string) => void;
	onSubmit: (e: FormEvent) => void;
}

export function QnaOfficialAnswerComposer({
	answerDraft,
	answerBusy,
	onDraftChange,
	onSubmit,
}: QnaOfficialAnswerComposerProps) {
	return (
		<form
			className="mt-8 rounded-xl border border-outline-variant bg-surface-container-low p-5"
			onSubmit={onSubmit}
			noValidate
		>
			<div className="mb-3 flex items-center gap-2">
				<Icon name="workspace_premium" className="text-primary" />
				<span className="text-label-md font-label-md text-on-surface">
					공식 답변 작성
				</span>
			</div>
			<textarea
				value={answerDraft}
				onChange={(e) => onDraftChange(e.target.value)}
				rows={5}
				required
				placeholder="구독자에게 공식 답변을 남겨 주세요"
				className="mb-3 w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-4 py-3 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
			/>
			<Button type="submit" fullWidth disabled={answerBusy}>
				{answerBusy ? "등록 중…" : "답변 등록"}
			</Button>
		</form>
	);
}
