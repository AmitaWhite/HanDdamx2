import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { EngagementBar } from "@/components/social/EngagementBar";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { useAuth } from "@/features/auth/AuthContext";
import { useSubmitState } from "@/features/auth/useSubmitState";
import {
	type BoardPostResponse,
	type BoardPostType,
	deletePremiumBoardPost,
	getPremiumBoardPost,
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

/** 숫자 postId → 실API(LDJ-003~005), 그 외 → 기존 mock UI */
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
	const { user } = useAuth();

	const [post, setPost] = useState<BoardPostResponse | null>(null);
	const [loading, setLoading] = useState(true);
	const [loadError, setLoadError] = useState<string | null>(null);

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

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		setLoadError(null);

		getPremiumBoardPost(postId)
			.then((data) => {
				if (cancelled) return;
				setPost(data);
				setEditTitle(data.title);
				setEditType(data.type);
				setEditContent(data.content);
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setLoadError(
					err instanceof ApiError
						? err.message
						: "게시글을 불러오지 못했습니다.",
				);
				setPost(null);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, [postId]);

	const isAuthor = !!user && !!post && user.memberId === post.memberId;
	const canEdit = isAuthor && post?.status === "WAITING";

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

			{(saveError || deleteError) && (
				<div className="mb-4">
					<Alert>{saveError ?? deleteError}</Alert>
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
					<div className="flex gap-3">
						<Button
							type="button"
							variant="secondary"
							fullWidth
							onClick={() => setEditing(false)}
							disabled={saving}
						>
							취소
						</Button>
						<Button type="submit" fullWidth disabled={saving}>
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
						회원 #{post.memberId} · {toRelativeLabel(post.createdAt)}
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
					<span
						className={
							"rounded px-2 py-1 text-[10px] font-bold " +
							(post.status === "answered"
								? "bg-primary text-on-primary"
								: "bg-surface-container text-secondary")
						}
					>
						{post.status === "answered" ? "답변 완료" : "답변 대기"}
					</span>
					<span className="text-caption font-caption text-secondary">
						{post.category}
					</span>
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
