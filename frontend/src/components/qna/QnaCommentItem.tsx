import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import type { BoardCommentResponse } from "@/features/board/boardApi";
import { formatRelativeTime } from "@/lib/relativeTime";

interface QnaCommentItemProps {
	comment: BoardCommentResponse;
	isReply?: boolean;
	isMine: boolean;
	canWriteComment: boolean;
	editing: boolean;
	editingDraft: string;
	replying: boolean;
	replyDraft: string;
	busy: boolean;
	onEditingDraftChange: (value: string) => void;
	onReplyDraftChange: (value: string) => void;
	onStartReply: () => void;
	onCancelReply: () => void;
	onSubmitReply: () => void;
	onStartEdit: () => void;
	onCancelEdit: () => void;
	onSaveEdit: () => void;
	onDelete: () => void;
}

export function QnaCommentItem({
	comment,
	isReply = false,
	isMine,
	canWriteComment,
	editing,
	editingDraft,
	replying,
	replyDraft,
	busy,
	onEditingDraftChange,
	onReplyDraftChange,
	onStartReply,
	onCancelReply,
	onSubmitReply,
	onStartEdit,
	onCancelEdit,
	onSaveEdit,
	onDelete,
}: QnaCommentItemProps) {
	return (
		<div className={isReply ? "ml-10 mt-3" : ""}>
			<div className="flex gap-3">
				<Avatar size={isReply ? 28 : 32} />
				<div className="min-w-0 flex-1">
					<div className="flex flex-wrap items-center gap-2">
						<span className="text-label-md font-label-md text-on-surface">
							{comment.memberNickname}
						</span>
						<span className="text-caption font-caption text-secondary">
							{formatRelativeTime(comment.createdAt)}
						</span>
					</div>

					{editing ? (
						<div className="mt-2 flex flex-col gap-2">
							<textarea
								value={editingDraft}
								onChange={(e) => onEditingDraftChange(e.target.value)}
								rows={3}
								maxLength={1000}
								className="w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-3 py-2 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
							/>
							<div className="flex gap-2">
								<Button
									type="button"
									variant="secondary"
									size="sm"
									disabled={busy}
									onClick={onCancelEdit}
								>
									취소
								</Button>
								<Button
									type="button"
									size="sm"
									disabled={busy}
									onClick={onSaveEdit}
								>
									{busy ? "저장 중…" : "저장"}
								</Button>
							</div>
						</div>
					) : (
						<p
							className={
								"mt-1 text-body-md " +
								(comment.deleted
									? "italic text-secondary"
									: "text-on-surface")
							}
						>
							{comment.deleted ? "삭제된 댓글입니다." : comment.content}
						</p>
					)}

					{!comment.deleted && !editing && (
						<div className="mt-1.5 flex flex-wrap gap-3">
							{!isReply && canWriteComment && (
								<button
									type="button"
									className="text-caption font-caption text-secondary hover:text-primary"
									onClick={onStartReply}
								>
									답글
								</button>
							)}
							{isMine && (
								<>
									<button
										type="button"
										className="text-caption font-caption text-secondary hover:text-primary"
										onClick={onStartEdit}
									>
										수정
									</button>
									<button
										type="button"
										className="text-caption font-caption text-secondary hover:text-primary"
										disabled={busy}
										onClick={onDelete}
									>
										삭제
									</button>
								</>
							)}
						</div>
					)}

					{!isReply && replying && (
						<div className="mt-3 flex items-start gap-2">
							<input
								value={replyDraft}
								onChange={(e) => onReplyDraftChange(e.target.value)}
								onKeyDown={(e) => {
									if (e.key === "Enter") {
										e.preventDefault();
										onSubmitReply();
									}
								}}
								placeholder="답글을 입력하세요"
								maxLength={1000}
								className="h-10 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
							/>
							<Button
								size="sm"
								variant="secondary"
								disabled={busy}
								onClick={onCancelReply}
							>
								취소
							</Button>
							<Button size="sm" disabled={busy} onClick={onSubmitReply}>
								등록
							</Button>
						</div>
					)}
				</div>
			</div>
		</div>
	);
}
