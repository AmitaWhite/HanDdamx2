import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import type { BoardCommentResponse } from "@/features/board/boardApi";
import { countComments } from "@/features/qna/qnaUtils";
import { QnaCommentItem } from "./QnaCommentItem";

interface QnaCommentSectionProps {
	comments: BoardCommentResponse[];
	canWriteComment: boolean;
	myMemberId: number | undefined;
	commentDraft: string;
	replyToId: number | null;
	replyDraft: string;
	editingCommentId: number | null;
	editingCommentDraft: string;
	commentBusy: boolean;
	onCommentDraftChange: (value: string) => void;
	onReplyDraftChange: (value: string) => void;
	onEditingDraftChange: (value: string) => void;
	onCreateComment: () => void;
	onStartReply: (parentId: number) => void;
	onCancelReply: () => void;
	onSubmitReply: (parentId: number) => void;
	onStartEditComment: (commentId: number, content: string) => void;
	onCancelEditComment: () => void;
	onSaveEditComment: (commentId: number) => void;
	onDeleteComment: (commentId: number) => void;
}

export function QnaCommentSection({
	comments,
	canWriteComment,
	myMemberId,
	commentDraft,
	replyToId,
	replyDraft,
	editingCommentId,
	editingCommentDraft,
	commentBusy,
	onCommentDraftChange,
	onReplyDraftChange,
	onEditingDraftChange,
	onCreateComment,
	onStartReply,
	onCancelReply,
	onSubmitReply,
	onStartEditComment,
	onCancelEditComment,
	onSaveEditComment,
	onDeleteComment,
}: QnaCommentSectionProps) {
	function renderComment(comment: BoardCommentResponse, isReply = false) {
		const isMine = !!myMemberId && myMemberId === comment.memberId;

		return (
			<div key={comment.id}>
				<QnaCommentItem
					comment={comment}
					isReply={isReply}
					isMine={isMine}
					canWriteComment={canWriteComment}
					editing={editingCommentId === comment.id}
					editingDraft={editingCommentDraft}
					replying={!isReply && replyToId === comment.id}
					replyDraft={replyDraft}
					busy={commentBusy}
					onEditingDraftChange={onEditingDraftChange}
					onReplyDraftChange={onReplyDraftChange}
					onStartReply={() => onStartReply(comment.id)}
					onCancelReply={onCancelReply}
					onSubmitReply={() => onSubmitReply(comment.id)}
					onStartEdit={() => onStartEditComment(comment.id, comment.content)}
					onCancelEdit={onCancelEditComment}
					onSaveEdit={() => onSaveEditComment(comment.id)}
					onDelete={() => onDeleteComment(comment.id)}
				/>
				{(comment.replies ?? []).map((reply) => renderComment(reply, true))}
			</div>
		);
	}

	return (
		<section className="mt-10">
			<h2 className="mb-4 text-label-md font-label-md text-on-surface">
				댓글 {countComments(comments)}
			</h2>

			{comments.length === 0 ? (
				<p className="mb-5 text-body-md text-secondary">
					아직 댓글이 없습니다.
				</p>
			) : (
				<div className="mb-5 flex flex-col gap-5">
					{comments.map((c) => renderComment(c))}
				</div>
			)}

			{canWriteComment ? (
				<div className="flex items-center gap-3">
					<Avatar size={32} />
					<input
						value={commentDraft}
						onChange={(e) => onCommentDraftChange(e.target.value)}
						onKeyDown={(e) => {
							if (e.key === "Enter") {
								e.preventDefault();
								onCreateComment();
							}
						}}
						placeholder="댓글을 입력하세요"
						maxLength={1000}
						disabled={commentBusy}
						className="h-11 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary disabled:opacity-50"
					/>
					<Button
						size="sm"
						disabled={commentBusy}
						onClick={onCreateComment}
					>
						{commentBusy ? "등록 중…" : "등록"}
					</Button>
				</div>
			) : (
				<p className="text-body-md text-secondary">
					댓글을 작성하려면 로그인해 주세요.
				</p>
			)}
		</section>
	);
}
