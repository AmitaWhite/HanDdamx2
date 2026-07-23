import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { MockQnaPostView } from "@/components/qna/MockQnaPostView";
import { QnaBackLink } from "@/components/qna/QnaBackLink";
import { QnaCommentSection } from "@/components/qna/QnaCommentSection";
import { QnaOfficialAnswerComposer } from "@/components/qna/QnaOfficialAnswerComposer";
import { QnaOfficialAnswerSection } from "@/components/qna/QnaOfficialAnswerSection";
import { QnaPostAlertBanner } from "@/components/qna/QnaPostAlertBanner";
import { QnaPostArticle } from "@/components/qna/QnaPostArticle";
import { QnaPostEditForm } from "@/components/qna/QnaPostEditForm";
import { Alert } from "@/components/ui/Alert";
import { useAuth } from "@/features/auth/AuthContext";
import { useQnaAnswer } from "@/features/qna/useQnaAnswer";
import { useQnaComments } from "@/features/qna/useQnaComments";
import { useQnaPostEdit } from "@/features/qna/useQnaPostEdit";
import { useRemoteQnaPost } from "@/features/qna/useRemoteQnaPost";
import { toNumericPostId } from "@/features/qna/qnaUtils";
import { findQnaPost } from "@/mocks/qna";

/** 숫자 postId → 실API(LDJ-003~005, 007~011), 그 외 → 기존 mock UI */
export function QnaPostPage() {
	const { postId = "" } = useParams();
	const numericPostId = toNumericPostId(postId);

	if (numericPostId !== null) {
		return <RemoteQnaPostPage postId={numericPostId} />;
	}
	return <MockQnaPostPage postId={postId} />;
}

function RemoteQnaPostPage({ postId }: { postId: number }) {
	const { user } = useAuth();
	const {
		post,
		setPost,
		answer,
		setAnswer,
		comments,
		setComments,
		loading,
		loadError,
		imageUploadFailed,
		permissions,
	} = useRemoteQnaPost(postId);

	const postEdit = useQnaPostEdit({ postId, post, setPost });
	const answerActions = useQnaAnswer({
		postId,
		setPost,
		answer,
		setAnswer,
	});
	const commentActions = useQnaComments({ postId, setComments });

	if (loading) {
		return (
			<div className="container-page max-w-2xl py-10 text-center text-body-md text-secondary">
				불러오는 중…
			</div>
		);
	}

	if (loadError || !post) {
		return (
			<div className="container-page max-w-2xl py-6">
				<Alert>{loadError ?? "게시글을 찾을 수 없습니다."}</Alert>
				<Link
					to={paths.home}
					className="mt-4 inline-block text-label-md font-label-md text-primary hover:underline"
				>
					홈으로
				</Link>
			</div>
		);
	}

	return (
		<div className="container-page max-w-2xl py-6">
			<QnaBackLink creatorId={post.creatorId} />

			<QnaPostAlertBanner
				saveError={postEdit.saveError}
				deleteError={postEdit.deleteError}
				imageError={postEdit.imageError}
				answerError={answerActions.answerError}
				commentError={commentActions.commentError}
				imageUploadFailed={imageUploadFailed}
			/>

			{postEdit.editing ? (
				<QnaPostEditForm
					post={post}
					editTitle={postEdit.editTitle}
					editType={postEdit.editType}
					editContent={postEdit.editContent}
					saving={postEdit.saving}
					imageBusy={postEdit.imageBusy}
					onTitleChange={postEdit.setEditTitle}
					onTypeChange={postEdit.setEditType}
					onContentChange={postEdit.setEditContent}
					onSave={postEdit.onSave}
					onCancel={postEdit.cancelEdit}
					onAddImages={postEdit.onAddImages}
					onRemoveImage={postEdit.onRemoveImage}
				/>
			) : (
				<QnaPostArticle
					post={post}
					canEdit={permissions.canEdit}
					isAuthor={permissions.isAuthor}
					deleting={postEdit.deleting}
					onEdit={postEdit.startEdit}
					onDelete={postEdit.onDelete}
				/>
			)}

			{!postEdit.editing && answer && (
				<QnaOfficialAnswerSection
					answer={answer}
					answerDraft={answerActions.answerDraft}
					answerEditing={answerActions.answerEditing}
					answerBusy={answerActions.answerBusy}
					canManageAnswer={permissions.canManageAnswer}
					onDraftChange={answerActions.setAnswerDraft}
					onStartEdit={answerActions.startAnswerEdit}
					onCancelEdit={answerActions.cancelAnswerEdit}
					onUpdate={answerActions.onUpdateAnswer}
					onDelete={answerActions.onDeleteAnswer}
				/>
			)}

			{!postEdit.editing && permissions.canWriteAnswer && (
				<QnaOfficialAnswerComposer
					answerDraft={answerActions.answerDraft}
					answerBusy={answerActions.answerBusy}
					onDraftChange={answerActions.setAnswerDraft}
					onSubmit={answerActions.onCreateAnswer}
				/>
			)}

			{!postEdit.editing && (
				<QnaCommentSection
					comments={comments}
					canWriteComment={permissions.canWriteComment}
					myMemberId={user?.memberId}
					commentDraft={commentActions.commentDraft}
					replyToId={commentActions.replyToId}
					replyDraft={commentActions.replyDraft}
					editingCommentId={commentActions.editingCommentId}
					editingCommentDraft={commentActions.editingCommentDraft}
					commentBusy={commentActions.commentBusy}
					onCommentDraftChange={commentActions.setCommentDraft}
					onReplyDraftChange={commentActions.setReplyDraft}
					onEditingDraftChange={commentActions.setEditingCommentDraft}
					onCreateComment={() => void commentActions.onCreateComment()}
					onStartReply={commentActions.startReply}
					onCancelReply={commentActions.cancelReply}
					onSubmitReply={(parentId) =>
						void commentActions.onCreateReply(parentId)
					}
					onStartEditComment={commentActions.startEditComment}
					onCancelEditComment={commentActions.cancelEditComment}
					onSaveEditComment={(commentId) =>
						void commentActions.onUpdateComment(commentId)
					}
					onDeleteComment={(commentId) =>
						void commentActions.onDeleteComment(commentId)
					}
				/>
			)}
		</div>
	);
}

function MockQnaPostPage({ postId }: { postId: string }) {
	const post = findQnaPost(postId);
	return <MockQnaPostView post={post} />;
}
