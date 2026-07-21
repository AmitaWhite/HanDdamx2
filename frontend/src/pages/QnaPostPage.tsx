import { useEffect, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { EngagementBar } from "@/components/social/EngagementBar";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { useAuth } from "@/features/auth/AuthContext";
import { useSubmitState } from "@/features/auth/useSubmitState";
import {
	type BoardAnswerResponse,
	type BoardCommentResponse,
	type BoardPostResponse,
	type BoardPostType,
	addPremiumBoardPostImages,
	createBoardAnswer,
	createBoardComment,
	createBoardCommentReply,
	deleteBoardAnswer,
	deleteBoardComment,
	deletePremiumBoardPost,
	deletePremiumBoardPostImage,
	getBoardAnswer,
	getBoardComments,
	getPremiumBoardPost,
	updateBoardAnswer,
	updateBoardComment,
	updatePremiumBoardPost,
} from "@/features/board/boardApi";
import { ApiError } from "@/lib/api";
import { commentsFor } from "@/mocks/comments";
import { mockImg } from "@/mocks/helpers";
import { findQnaPost, mockQnaAnswers, type QnaCategory } from "@/mocks/qna";

const TYPE_TO_CATEGORY: Record<BoardPostType, QnaCategory> = {
	QUESTION: "제작 질문",
	FEEDBACK: "작품 피드백",
	CONTENT_SUGGESTION: "콘텐츠 제안",
	MATERIAL: "재료 추천",
	GENERAL: "일반 소통",
};

const CATEGORY_OPTIONS: { label: QnaCategory; type: BoardPostType }[] = [
	{ label: "제작 질문", type: "QUESTION" },
	{ label: "작품 피드백", type: "FEEDBACK" },
	{ label: "콘텐츠 제안", type: "CONTENT_SUGGESTION" },
	{ label: "재료 추천", type: "MATERIAL" },
	{ label: "일반 소통", type: "GENERAL" },
];

function toNumericPostId(value: string): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isInteger(n) && n > 0 ? n : null;
}

function toRelativeLabel(iso: string): string {
	const diffMs = Date.now() - new Date(iso).getTime();
	if (Number.isNaN(diffMs) || diffMs < 0) return "방금";
	const minutes = Math.floor(diffMs / 60_000);
	if (minutes < 1) return "방금";
	if (minutes < 60) return `${minutes}분 전`;
	const hours = Math.floor(minutes / 60);
	if (hours < 24) return `${hours}시간 전`;
	const days = Math.floor(hours / 24);
	if (days < 7) return `${days}일 전`;
	return `${Math.floor(days / 7)}주 전`;
}

function countComments(comments: BoardCommentResponse[]): number {
	return comments.reduce(
		(sum, c) => sum + 1 + (c.replies?.length ?? 0),
		0,
	);
}

function upsertRootComment(
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

function markCommentDeleted(
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

function appendReply(
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

function replaceCommentContent(
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
	const navigate = useNavigate();
	const location = useLocation();
	const { user } = useAuth();

	const [post, setPost] = useState<BoardPostResponse | null>(null);
	const [answer, setAnswer] = useState<BoardAnswerResponse | null>(null);
	const [comments, setComments] = useState<BoardCommentResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [loadError, setLoadError] = useState<string | null>(null);
	const imageUploadFailed =
		(location.state as { imageUploadFailed?: boolean } | null)?.imageUploadFailed ===
		true;

	const [editing, setEditing] = useState(false);
	const [editTitle, setEditTitle] = useState("");
	const [editType, setEditType] = useState<BoardPostType>("QUESTION");
	const [editContent, setEditContent] = useState("");

	const [answerDraft, setAnswerDraft] = useState("");
	const [answerEditing, setAnswerEditing] = useState(false);

	const [commentDraft, setCommentDraft] = useState("");
	const [replyToId, setReplyToId] = useState<number | null>(null);
	const [replyDraft, setReplyDraft] = useState("");
	const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
	const [editingCommentDraft, setEditingCommentDraft] = useState("");

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
	const {
		loading: answerBusy,
		error: answerError,
		run: runAnswer,
	} = useSubmitState("공식 답변 처리에 실패했습니다. 다시 시도해 주세요.");
	const {
		loading: commentBusy,
		error: commentError,
		run: runComment,
	} = useSubmitState("댓글 처리에 실패했습니다. 다시 시도해 주세요.");

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		setLoadError(null);

		Promise.all([
			getPremiumBoardPost(postId),
			getBoardAnswer(postId).catch(() => null),
			getBoardComments(postId).catch(() => [] as BoardCommentResponse[]),
		])
			.then(([data, answerData, commentData]) => {
				if (cancelled) return;
				setPost(data);
				setAnswer(answerData);
				setComments(commentData);
				setEditTitle(data.title);
				setEditType(data.type);
				setEditContent(data.content);
				setAnswerDraft(answerData?.content ?? "");
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setLoadError(
					err instanceof ApiError
						? err.message
						: "게시글을 불러오지 못했습니다.",
				);
				setPost(null);
				setAnswer(null);
				setComments([]);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, [postId]);

	const isAuthor = !!user && !!post && user.memberId === post.memberId;
	/** 게시판 소유 크리에이터만 공식 답변 작성·수정·삭제 */
	const isBoardOwner = !!user && !!post && user.memberId === post.creatorId;
	const canEdit = isAuthor && post?.status === "WAITING";
	const canWriteAnswer = isBoardOwner && post?.status === "WAITING" && !answer;
	const canManageAnswer = isBoardOwner && !!answer;
	const canWriteComment = !!user;

	function startEdit() {
		if (!post) return;
		setEditTitle(post.title);
		setEditType(post.type);
		setEditContent(post.content);
		setEditing(true);
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

	/** LDJ-007: 수정 중 이미지 추가 — 선택 즉시 업로드 */
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
				prev
					? { ...prev, images: [...prev.images, ...added] }
					: prev,
			);
		}
	}

	/** LDJ-008: 수정 중 이미지 삭제 */
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

	/** LDJ-009: 공식 답변 작성 */
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

	/** LDJ-010: 공식 답변 수정 */
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

	/** LDJ-011: 공식 답변 삭제 */
	async function onDeleteAnswer() {
		if (!answer) return;
		if (!window.confirm("공식 답변을 삭제할까요? 게시글이 답변 대기로 돌아갑니다.")) {
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

	/** LDJ-013: 일반 댓글 작성 */
	async function onCreateComment() {
		const content = commentDraft.trim();
		if (!content) return;
		const created = await runComment(() => createBoardComment(postId, content));
		if (created) {
			setComments((prev) => upsertRootComment(prev, created));
			setCommentDraft("");
		}
	}

	/** LDJ-014: 대댓글 작성 */
	async function onCreateReply(parentId: number) {
		const content = replyDraft.trim();
		if (!content) return;
		const created = await runComment(() =>
			createBoardCommentReply(parentId, content),
		);
		if (created) {
			setComments((prev) => appendReply(prev, parentId, created));
			setReplyToId(null);
			setReplyDraft("");
		}
	}

	/** LDJ-015: 댓글·대댓글 수정 */
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
			setEditingCommentId(null);
			setEditingCommentDraft("");
		}
	}

	/** LDJ-016: 댓글·대댓글 삭제 */
	async function onDeleteComment(commentId: number) {
		if (!window.confirm("이 댓글을 삭제할까요?")) return;
		const ok = await runComment(async () => {
			await deleteBoardComment(commentId);
			return true;
		});
		if (ok) {
			setComments((prev) => markCommentDeleted(prev, commentId));
			if (editingCommentId === commentId) {
				setEditingCommentId(null);
				setEditingCommentDraft("");
			}
			if (replyToId === commentId) {
				setReplyToId(null);
				setReplyDraft("");
			}
		}
	}

	function renderCommentItem(
		comment: BoardCommentResponse,
		isReply = false,
	) {
		const mine = !!user && user.memberId === comment.memberId;
		const editingThis = editingCommentId === comment.id;

		return (
			<div key={comment.id} className={isReply ? "ml-10 mt-3" : ""}>
				<div className="flex gap-3">
					<Avatar size={isReply ? 28 : 32} />
					<div className="min-w-0 flex-1">
						<div className="flex flex-wrap items-center gap-2">
							<span className="text-label-md font-label-md text-on-surface">
								{comment.memberNickname}
							</span>
							<span className="text-caption font-caption text-secondary">
								{toRelativeLabel(comment.createdAt)}
							</span>
						</div>

						{editingThis ? (
							<div className="mt-2 flex flex-col gap-2">
								<textarea
									value={editingCommentDraft}
									onChange={(e) => setEditingCommentDraft(e.target.value)}
									rows={3}
									maxLength={1000}
									className="w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-3 py-2 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
								/>
								<div className="flex gap-2">
									<Button
										type="button"
										variant="secondary"
										size="sm"
										disabled={commentBusy}
										onClick={() => {
											setEditingCommentId(null);
											setEditingCommentDraft("");
										}}
									>
										취소
									</Button>
									<Button
										type="button"
										size="sm"
										disabled={commentBusy}
										onClick={() => onUpdateComment(comment.id)}
									>
										{commentBusy ? "저장 중…" : "저장"}
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

						{!comment.deleted && !editingThis && (
							<div className="mt-1.5 flex flex-wrap gap-3">
								{!isReply && canWriteComment && (
									<button
										type="button"
										className="text-caption font-caption text-secondary hover:text-primary"
										onClick={() => {
											setReplyToId(comment.id);
											setReplyDraft("");
											setEditingCommentId(null);
										}}
									>
										답글
									</button>
								)}
								{mine && (
									<>
										<button
											type="button"
											className="text-caption font-caption text-secondary hover:text-primary"
											onClick={() => {
												setEditingCommentId(comment.id);
												setEditingCommentDraft(comment.content);
												setReplyToId(null);
											}}
										>
											수정
										</button>
										<button
											type="button"
											className="text-caption font-caption text-secondary hover:text-primary"
											disabled={commentBusy}
											onClick={() => onDeleteComment(comment.id)}
										>
											삭제
										</button>
									</>
								)}
							</div>
						)}

						{!isReply && replyToId === comment.id && (
							<div className="mt-3 flex items-start gap-2">
								<input
									value={replyDraft}
									onChange={(e) => setReplyDraft(e.target.value)}
									onKeyDown={(e) => {
										if (e.key === "Enter") {
											e.preventDefault();
											void onCreateReply(comment.id);
										}
									}}
									placeholder="답글을 입력하세요"
									maxLength={1000}
									className="h-10 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
								/>
								<Button
									size="sm"
									variant="secondary"
									disabled={commentBusy}
									onClick={() => {
										setReplyToId(null);
										setReplyDraft("");
									}}
								>
									취소
								</Button>
								<Button
									size="sm"
									disabled={commentBusy}
									onClick={() => onCreateReply(comment.id)}
								>
									등록
								</Button>
							</div>
						)}
					</div>
				</div>

				{(comment.replies ?? []).map((reply) =>
					renderCommentItem(reply, true),
				)}
			</div>
		);
	}

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
			<Link
				to={paths.creatorQna(post.creatorId)}
				className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
			>
				<Icon name="arrow_back" className="text-[18px]" />
				Q&A 게시판
			</Link>

			{(saveError ||
				deleteError ||
				imageError ||
				answerError ||
				commentError ||
				imageUploadFailed) && (
				<div className="mb-4">
					<Alert>
						{saveError ??
							deleteError ??
							imageError ??
							answerError ??
							commentError ??
							"글은 등록됐지만 이미지가 저장되지 않았습니다. 아래에서 다시 추가해 주세요."}
					</Alert>
				</div>
			)}

			{editing ? (
				<form className="flex flex-col gap-4" onSubmit={onSave} noValidate>
					<div className="flex flex-col gap-1.5">
						<label
							htmlFor="edit-type"
							className="text-label-md font-label-md text-on-surface"
						>
							카테고리
						</label>
						<select
							id="edit-type"
							value={editType}
							onChange={(e) => setEditType(e.target.value as BoardPostType)}
							className="h-11 rounded border border-outline-variant bg-surface-container-lowest px-4 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
						>
							{CATEGORY_OPTIONS.map((opt) => (
								<option key={opt.type} value={opt.type}>
									{opt.label}
								</option>
							))}
						</select>
					</div>
					<Input
						label="제목"
						value={editTitle}
						onChange={(e) => setEditTitle(e.target.value)}
						maxLength={255}
						required
					/>
					<div className="flex flex-col gap-1.5">
						<label
							htmlFor="edit-content"
							className="text-label-md font-label-md text-on-surface"
						>
							내용
						</label>
						<textarea
							id="edit-content"
							value={editContent}
							onChange={(e) => setEditContent(e.target.value)}
							rows={8}
							required
							className="w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-4 py-3 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
						/>
					</div>

					<div>
						<label className="mb-2 block text-label-md font-label-md text-on-surface">
							이미지
						</label>
						{post.images.length > 0 && (
							<ul className="mb-3 flex flex-wrap gap-3">
								{[...post.images]
									.sort((a, b) => a.orderIndex - b.orderIndex)
									.map((image) => (
										<li key={image.id} className="relative">
											<img
												src={image.url}
												alt={image.originalName ?? "첨부 이미지"}
												className="h-24 w-24 rounded-lg object-cover"
											/>
											<button
												type="button"
												onClick={() => onRemoveImage(image.id)}
												disabled={imageBusy}
												className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-on-surface text-surface-container-lowest disabled:opacity-50"
												aria-label="이미지 삭제"
											>
												<Icon name="close" className="text-[14px]" />
											</button>
										</li>
									))}
							</ul>
						)}
						<label className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-outline-variant bg-surface-container-low py-6 text-center hover:border-primary">
							<Icon name="upload" className="text-[24px] text-secondary" />
							<span className="text-body-md text-secondary">
								{imageBusy ? "업로드 중…" : "이미지 추가"}
							</span>
							<input
								type="file"
								multiple
								accept="image/*"
								className="hidden"
								disabled={imageBusy || saving}
								onChange={onAddImages}
							/>
						</label>
					</div>

					<div className="flex gap-3">
						<Button
							type="button"
							variant="secondary"
							fullWidth
							onClick={() => setEditing(false)}
							disabled={saving || imageBusy}
						>
							취소
						</Button>
						<Button type="submit" fullWidth disabled={saving || imageBusy}>
							{saving ? "저장 중…" : "저장"}
						</Button>
					</div>
				</form>
			) : (
				<article>
					<div className="mb-3 flex items-center justify-between gap-3">
						<div className="flex items-center gap-2">
							<span
								className={
									"rounded px-2 py-1 text-[10px] font-bold " +
									(post.status === "ANSWERED"
										? "bg-primary text-on-primary"
										: "bg-surface-container text-secondary")
								}
							>
								{post.status === "ANSWERED" ? "답변 완료" : "답변 대기"}
							</span>
							<span className="text-caption font-caption text-secondary">
								{TYPE_TO_CATEGORY[post.type]}
							</span>
						</div>
						{isAuthor && (
							<div className="flex gap-2">
								{canEdit && (
									<Button type="button" variant="secondary" size="sm" onClick={startEdit}>
										수정
									</Button>
								)}
								<Button
									type="button"
									variant="outline"
									size="sm"
									onClick={onDelete}
									disabled={deleting}
								>
									{deleting ? "삭제 중…" : "삭제"}
								</Button>
							</div>
						)}
					</div>
					<h1 className="mb-3 text-headline-lg font-display text-on-surface">
						{post.title}
					</h1>
					<div className="mb-5 flex items-center gap-2 text-caption font-caption text-secondary">
						<Avatar size={24} />
						{post.memberNickname} · {toRelativeLabel(post.createdAt)}
					</div>
					<div className="mb-5 whitespace-pre-wrap text-body-md text-on-surface">
						{post.content}
					</div>
					{post.images.length > 0 && (
						<div className="mb-5 grid grid-cols-2 gap-3">
							{[...post.images]
								.sort((a, b) => a.orderIndex - b.orderIndex)
								.map((image) => (
									<img
										key={image.id}
										src={image.url}
										alt={image.originalName ?? "첨부 이미지"}
										className="aspect-square rounded-lg object-cover"
									/>
								))}
						</div>
					)}
				</article>
			)}

			{/* LDJ-009~011 + GET: 공식 답변 */}
			{!editing && answer && (
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
									onClick={() => {
										setAnswerDraft(answer.content);
										setAnswerEditing(true);
									}}
								>
									수정
								</Button>
								<Button
									type="button"
									variant="outline"
									size="sm"
									onClick={onDeleteAnswer}
									disabled={answerBusy}
								>
									{answerBusy ? "삭제 중…" : "삭제"}
								</Button>
							</div>
						)}
					</div>
					{answerEditing ? (
						<form className="flex flex-col gap-3" onSubmit={onUpdateAnswer} noValidate>
							<textarea
								value={answerDraft}
								onChange={(e) => setAnswerDraft(e.target.value)}
								rows={5}
								required
								className="w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-4 py-3 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
							/>
							<div className="flex gap-3">
								<Button
									type="button"
									variant="secondary"
									fullWidth
									onClick={() => {
										setAnswerDraft(answer.content);
										setAnswerEditing(false);
									}}
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
								{toRelativeLabel(answer.createdAt)}
							</p>
						</>
					)}
				</div>
			)}

			{!editing && canWriteAnswer && (
				<form
					className="mt-8 rounded-xl border border-outline-variant bg-surface-container-low p-5"
					onSubmit={onCreateAnswer}
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
						onChange={(e) => setAnswerDraft(e.target.value)}
						rows={5}
						required
						placeholder="구독자에게 공식 답변을 남겨 주세요"
						className="mb-3 w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-4 py-3 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					/>
					<Button type="submit" fullWidth disabled={answerBusy}>
						{answerBusy ? "등록 중…" : "답변 등록"}
					</Button>
				</form>
			)}

			{/* LDJ-012~016: 댓글·대댓글 */}
			{!editing && (
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
							{comments.map((c) => renderCommentItem(c))}
						</div>
					)}

					{canWriteComment ? (
						<div className="flex items-center gap-3">
							<Avatar size={32} />
							<input
								value={commentDraft}
								onChange={(e) => setCommentDraft(e.target.value)}
								onKeyDown={(e) => {
									if (e.key === "Enter") {
										e.preventDefault();
										void onCreateComment();
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
								onClick={() => void onCreateComment()}
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
			)}
		</div>
	);
}

function MockQnaPostPage({ postId }: { postId: string }) {
	const post = findQnaPost(postId);
	const answer = mockQnaAnswers[post.id];
	const [comments, setComments] = useState(commentsFor(post.id));
	const [liked, setLiked] = useState(false);
	const [draft, setDraft] = useState("");

	function submitComment() {
		if (!draft.trim()) return;
		setComments((prev) => [
			...prev,
			{
				id: `local-${Date.now()}`,
				postId: post.id,
				authorName: "나",
				avatarSeed: "member1",
				isPaidSubscriber: false,
				body: draft.trim(),
				createdAtLabel: "방금",
			},
		]);
		setDraft("");
	}

	return (
		<div className="container-page max-w-2xl py-6">
			<Link
				to={paths.creatorQna(post.creatorId)}
				className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
			>
				<Icon name="arrow_back" className="text-[18px]" />
				Q&A 게시판
			</Link>

			<article>
				<div className="mb-3 flex items-center gap-2">
					<Chip active={post.status === "answered"} size="sm">
						{post.status === "answered" ? "답변 완료" : "답변 대기"}
					</Chip>
					<span className="text-caption font-caption text-secondary">{post.category}</span>
				</div>
				<h1 className="mb-3 text-headline-lg font-display text-on-surface">
					{post.title}
				</h1>
				<div className="mb-5 flex items-center gap-2 text-caption font-caption text-secondary">
					<Avatar size={24} />
					{post.authorName} · {post.createdAtLabel}
				</div>
				<div className="mb-5 flex flex-col gap-3 text-body-md text-on-surface">
					{post.body.map((paragraph) => (
						<p key={paragraph}>{paragraph}</p>
					))}
				</div>
				{post.attachedImageSeeds && (
					<div className="mb-5 grid grid-cols-2 gap-3">
						{post.attachedImageSeeds.map((seed) => (
							<img
								key={seed}
								src={mockImg(seed, 400, 400)}
								alt="첨부 이미지"
								className="aspect-square rounded-lg object-cover"
							/>
						))}
					</div>
				)}
				<EngagementBar
					likeCount={24 + (liked ? 1 : 0)}
					commentCount={comments.length}
					liked={liked}
					onToggleLike={() => setLiked((v) => !v)}
					className="mb-6 border-b border-outline-variant/50 pb-5"
				/>
			</article>

			{answer && (
				<div className="mb-8 rounded-xl border border-primary/30 bg-primary/5 p-5">
					<div className="mb-3 flex items-center gap-2">
						<Icon name="workspace_premium" className="text-primary" />
						<Avatar src={mockImg(answer.avatarSeed, 80, 80)} size={28} />
						<span className="text-label-md font-label-md text-on-surface">
							{answer.creatorName}의 답변
						</span>
					</div>
					<div className="flex flex-col gap-2 text-body-md text-on-surface">
						{answer.body.map((paragraph) => (
							<p key={paragraph}>{paragraph}</p>
						))}
					</div>
					<p className="mt-3 text-caption font-caption text-secondary">
						{answer.answeredAtLabel}
					</p>
				</div>
			)}

			<h2 className="mb-4 text-label-md font-label-md text-on-surface">
				댓글 {comments.length}
			</h2>
			<div className="mb-5 flex flex-col gap-4">
				{comments.map((c) => (
					<div key={c.id} className="flex gap-3">
						<Avatar src={mockImg(c.avatarSeed, 80, 80)} size={32} />
						<div>
							<div className="flex items-center gap-2">
								<span className="text-label-md font-label-md text-on-surface">
									{c.authorName}
								</span>
								{c.isPaidSubscriber && (
									<span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">
										구독자
									</span>
								)}
							</div>
							<p className="text-body-md text-on-surface">{c.body}</p>
							<p className="text-caption font-caption text-secondary">
								{c.createdAtLabel}
							</p>
						</div>
					</div>
				))}
			</div>

			<div className="flex items-center gap-3">
				<Avatar size={32} />
				<input
					value={draft}
					onChange={(e) => setDraft(e.target.value)}
					onKeyDown={(e) => e.key === "Enter" && submitComment()}
					placeholder="댓글을 입력하세요"
					className="h-11 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
				/>
				<Button size="sm" onClick={submitComment}>
					등록
				</Button>
			</div>
		</div>
	);
}
