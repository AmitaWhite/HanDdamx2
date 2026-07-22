import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { EngagementBar } from "@/components/social/EngagementBar";
import { Icon } from "@/components/ui/Icon";
import { MessageDialog } from "@/components/ui/MessageDialog";
import {
	createFeedComment,
	createFeedCommentReply,
	deleteFeedComment,
	getFeedComments,
	updateFeedComment,
} from "@/features/comment/commentApi";
import type { FeedCommentResponse } from "@/features/comment/types";
import { useAuth } from "@/features/auth/AuthContext";
import { getFeed, getFeedAttachments } from "@/features/feed/feedApi";
import type { AttachmentResponse, FeedDetailResponse } from "@/features/feed/types";
import { likeFeed, unlikeFeed } from "@/features/like/likeApi";
import {
	changePollVote,
	closePoll,
	deletePoll,
	getPoll,
	getPollResults,
	updatePoll,
	votePoll,
} from "@/features/poll/pollApi";
import type { PollResponse, PollResultResponse } from "@/features/poll/types";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";
import { endOfDayToIso, formatDateLabel, toDateInputValue } from "@/lib/date";
import { renderFormattedContent } from "@/lib/richText";

const REQUIRED_LEVEL_LABEL: Record<string, string> = {
	FREE_SUBSCRIBER: "무료 구독자",
	PAID_SUBSCRIBER: "유료 구독자",
};

function totalCommentCount(comments: FeedCommentResponse[]): number {
	return comments.reduce((sum, c) => sum + 1 + c.replies.length, 0);
}

export function PostDetailPage() {
	const { postId = "" } = useParams();
	const { user } = useAuth();

	const [feed, setFeed] = useState<FeedDetailResponse | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	const [attachments, setAttachments] = useState<AttachmentResponse[]>([]);
	const [slideIndex, setSlideIndex] = useState(0);

	const [likeCount, setLikeCount] = useState(0);
	const [liked, setLiked] = useState(false);
	const [likeError, setLikeError] = useState<string | null>(null);

	const [comments, setComments] = useState<FeedCommentResponse[]>([]);
	const [commentsError, setCommentsError] = useState<string | null>(null);
	const [draft, setDraft] = useState("");
	const [replyTarget, setReplyTarget] = useState<number | null>(null);
	const [replyDraft, setReplyDraft] = useState("");
	const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
	const [editDraft, setEditDraft] = useState("");
	const [commentValidationMessage, setCommentValidationMessage] = useState<string | null>(null);
	const [confirmDeleteCommentId, setConfirmDeleteCommentId] = useState<number | null>(null);

	const [poll, setPoll] = useState<PollResponse | null>(null);
	const [pollResults, setPollResults] = useState<PollResultResponse | null>(null);
	const [pollError, setPollError] = useState<string | null>(null);
	const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null);
	const [pollEditMode, setPollEditMode] = useState(false);
	const [pollSubmitting, setPollSubmitting] = useState(false);

	const [pollManageOpen, setPollManageOpen] = useState(false);
	const [editQuestion, setEditQuestion] = useState("");
	const [editEndAt, setEditEndAt] = useState("");
	const [pollActionError, setPollActionError] = useState<string | null>(null);
	const [pollActionSubmitting, setPollActionSubmitting] = useState(false);
	const [confirmClosePoll, setConfirmClosePoll] = useState(false);
	const [confirmDeletePoll, setConfirmDeletePoll] = useState(false);

	useEffect(() => {
		setLoading(true);
		getFeed(postId)
			.then((res) => {
				setFeed(res);
				setLikeCount(res.likeCount);
				setLiked(res.liked);
			})
			.catch((err) => {
				setError(
					err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.",
				);
			})
			.finally(() => setLoading(false));
	}, [postId]);

	useEffect(() => {
		if (!feed || feed.locked) return;
		getFeedAttachments(feed.id)
			.then(setAttachments)
			.catch(() => setAttachments([]));
		setSlideIndex(0);
	}, [feed]);

	useEffect(() => {
		if (!feed?.pollId) return;
		const pollId = feed.pollId;
		Promise.all([getPoll(pollId), getPollResults(pollId)])
			.then(([p, r]) => {
				setPoll(p);
				setPollResults(r);
				setSelectedOptionId(p.myVotedOptionId);
				setPollEditMode(p.myVotedOptionId == null);
			})
			.catch((err) => {
				setPollError(err instanceof ApiError ? err.message : "투표를 불러오지 못했습니다.");
			});
	}, [feed?.pollId]);

	useEffect(() => {
		if (!feed) return;
		getFeedComments(feed.id)
			.then(setComments)
			.catch((err) => {
				setCommentsError(
					err instanceof ApiError ? err.message : "댓글을 불러오지 못했습니다.",
				);
			});
	}, [feed]);

	async function refreshComments() {
		if (!feed) return;
		try {
			setComments(await getFeedComments(feed.id));
		} catch (err) {
			setCommentsError(err instanceof ApiError ? err.message : "댓글을 불러오지 못했습니다.");
		}
	}

	async function submitComment() {
		if (!feed) return;
		if (!draft.trim()) {
			setCommentValidationMessage("댓글 내용을 입력해 주세요.");
			return;
		}
		try {
			await createFeedComment(feed.id, draft.trim());
			setDraft("");
			await refreshComments();
		} catch (err) {
			setCommentsError(err instanceof ApiError ? err.message : "댓글 등록에 실패했습니다.");
		}
	}

	async function submitReply(parentCommentId: number) {
		if (!feed) return;
		if (!replyDraft.trim()) {
			setCommentValidationMessage("답글 내용을 입력해 주세요.");
			return;
		}
		try {
			await createFeedCommentReply(feed.id, parentCommentId, replyDraft.trim());
			setReplyDraft("");
			setReplyTarget(null);
			await refreshComments();
		} catch (err) {
			setCommentsError(err instanceof ApiError ? err.message : "답글 등록에 실패했습니다.");
		}
	}

	function startEditComment(comment: FeedCommentResponse) {
		setReplyTarget(null);
		setEditingCommentId(comment.id);
		setEditDraft(comment.content);
	}

	function cancelEditComment() {
		setEditingCommentId(null);
		setEditDraft("");
	}

	async function submitEditComment(commentId: number) {
		if (!feed) return;
		if (!editDraft.trim()) {
			setCommentValidationMessage("댓글 내용을 입력해 주세요.");
			return;
		}
		try {
			await updateFeedComment(feed.id, commentId, editDraft.trim());
			setEditingCommentId(null);
			setEditDraft("");
			await refreshComments();
		} catch (err) {
			setCommentsError(err instanceof ApiError ? err.message : "댓글 수정에 실패했습니다.");
		}
	}

	function handleDeleteComment(commentId: number) {
		if (!feed) return;
		setConfirmDeleteCommentId(commentId);
	}

	async function executeDeleteComment() {
		const commentId = confirmDeleteCommentId;
		setConfirmDeleteCommentId(null);
		if (!feed || commentId == null) return;
		try {
			await deleteFeedComment(feed.id, commentId);
			await refreshComments();
		} catch (err) {
			setCommentsError(err instanceof ApiError ? err.message : "댓글 삭제에 실패했습니다.");
		}
	}

	function selectPollOption(optionId: number) {
		if (!pollEditMode || poll?.closed) return;
		setSelectedOptionId(optionId);
	}

	function startPollEdit() {
		setPollError(null);
		setPollEditMode(true);
	}

	function cancelPollEdit() {
		if (!poll) return;
		setSelectedOptionId(poll.myVotedOptionId);
		setPollEditMode(false);
		setPollError(null);
	}

	async function submitPollVote() {
		if (!poll || selectedOptionId == null) return;
		if (poll.myVotedOptionId === selectedOptionId) {
			setPollEditMode(false);
			return;
		}
		setPollError(null);
		setPollSubmitting(true);
		try {
			if (poll.myVotedOptionId == null) {
				await votePoll(poll.pollId, selectedOptionId);
			} else {
				await changePollVote(poll.pollId, selectedOptionId);
			}
			const [freshPoll, freshResults] = await Promise.all([
				getPoll(poll.pollId),
				getPollResults(poll.pollId),
			]);
			setPoll(freshPoll);
			setPollResults(freshResults);
			setSelectedOptionId(freshPoll.myVotedOptionId);
			setPollEditMode(false);
		} catch (err) {
			setPollError(err instanceof ApiError ? err.message : "투표에 실패했습니다.");
		} finally {
			setPollSubmitting(false);
		}
	}

	function startPollManage() {
		if (!poll) return;
		setEditQuestion(poll.question);
		setEditEndAt(toDateInputValue(poll.endAt));
		setPollActionError(null);
		setPollManageOpen(true);
	}

	function cancelPollManage() {
		setPollManageOpen(false);
		setPollActionError(null);
	}

	async function submitPollEdit() {
		if (!poll) return;
		const trimmedQuestion = editQuestion.trim();
		if (!trimmedQuestion || !editEndAt) {
			setPollActionError("질문과 종료 일시를 입력해 주세요.");
			return;
		}
		setPollActionSubmitting(true);
		setPollActionError(null);
		try {
			await updatePoll(poll.pollId, {
				question: trimmedQuestion,
				endAt: endOfDayToIso(editEndAt),
			});
			const [freshPoll, freshResults] = await Promise.all([
				getPoll(poll.pollId),
				getPollResults(poll.pollId),
			]);
			setPoll(freshPoll);
			setPollResults(freshResults);
			setPollManageOpen(false);
		} catch (err) {
			setPollActionError(err instanceof ApiError ? err.message : "투표 수정에 실패했습니다.");
		} finally {
			setPollActionSubmitting(false);
		}
	}

	function handleClosePoll() {
		if (!poll) return;
		setConfirmClosePoll(true);
	}

	async function executeClosePoll() {
		setConfirmClosePoll(false);
		if (!poll) return;
		setPollActionSubmitting(true);
		setPollActionError(null);
		try {
			await closePoll(poll.pollId);
			const [freshPoll, freshResults] = await Promise.all([
				getPoll(poll.pollId),
				getPollResults(poll.pollId),
			]);
			setPoll(freshPoll);
			setPollResults(freshResults);
		} catch (err) {
			setPollActionError(err instanceof ApiError ? err.message : "투표 종료에 실패했습니다.");
		} finally {
			setPollActionSubmitting(false);
		}
	}

	function handleDeletePoll() {
		if (!poll) return;
		setConfirmDeletePoll(true);
	}

	async function executeDeletePoll() {
		setConfirmDeletePoll(false);
		if (!poll) return;
		setPollActionSubmitting(true);
		setPollActionError(null);
		try {
			await deletePoll(poll.pollId);
			setPoll(null);
			setPollResults(null);
			setPollManageOpen(false);
		} catch (err) {
			setPollActionError(err instanceof ApiError ? err.message : "투표 삭제에 실패했습니다.");
			setPollActionSubmitting(false);
		}
	}

	async function toggleLike() {
		if (!feed) return;
		setLikeError(null);
		try {
			const res = liked ? await unlikeFeed(feed.id) : await likeFeed(feed.id);
			setLiked(res.liked);
			setLikeCount(res.likeCount);
		} catch (err) {
			setLikeError(err instanceof ApiError ? err.message : "요청에 실패했습니다.");
		}
	}

	if (loading) {
		return <p className="container-page py-16 text-center text-body-md text-secondary">불러오는 중…</p>;
	}
	if (error || !feed) {
		return <p className="container-page py-16 text-center text-body-md text-secondary">{error ?? "게시물을 찾을 수 없어요."}</p>;
	}

	const isOwner = user?.memberId === feed.creator.creatorId;
	const mediaAttachments = attachments.filter(
		(a) => a.type === "IMAGE" || a.type === "VIDEO_LINK" || (a.mimeType?.startsWith("video/") ?? false),
	);
	const fileAttachments = attachments.filter((a) => !mediaAttachments.includes(a));

	return (
		<>
		<div className="container-page grid grid-cols-1 gap-gutter py-6 lg:grid-cols-[1fr_400px]">
			<div className="min-w-0">
				<Link
					to={paths.home}
					className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
				>
					<Icon name="arrow_back" className="text-[18px]" />
					돌아가기
				</Link>

				<div className="mb-4 flex items-center gap-3">
					<Link to={paths.creator(feed.creator.creatorId)}>
						<Avatar
							src={feed.creator.profileImageUrl ?? undefined}
							fallbackText={feed.creator.nickname}
							size={40}
						/>
					</Link>
					<div className="min-w-0">
						<div className="flex items-center gap-2">
							<Link
								to={paths.creator(feed.creator.creatorId)}
								className="text-label-md font-label-md text-on-surface hover:text-primary"
							>
								{feed.creator.nickname}
							</Link>
							<span className="text-caption font-caption text-secondary">
								{formatDateLabel(feed.createdAt)}
							</span>
						</div>
						{feed.visibility !== "PUBLIC" && (
							<Chip active size="sm" className="mt-1">
								{feed.visibility === "PAID_SUBSCRIBER" ? "유료 구독자 공개" : "무료 구독자 공개"}
							</Chip>
						)}
					</div>
				</div>

				{feed.locked ? (
					<div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-outline-variant py-16 text-center">
						<Icon name="lock" className="text-[32px] text-secondary" />
						<p className="text-body-md text-on-surface">
							{REQUIRED_LEVEL_LABEL[feed.requiredLevel ?? ""] ?? "구독자"}만 볼 수 있는 게시물이에요
						</p>
						<Link
							to={paths.subscribeSelect(feed.creator.creatorId)}
							className="text-label-md font-label-md text-primary hover:underline"
						>
							구독하러 가기
						</Link>
					</div>
				) : (
					<>
						{mediaAttachments.length > 0 && (
							<div className="relative mb-4 overflow-hidden rounded-xl bg-surface-container-low">
								<div
									className="flex transition-transform duration-300 ease-out"
									style={{ transform: `translateX(-${slideIndex * 100}%)` }}
								>
									{mediaAttachments.map((a) => {
										const isVideo = a.type === "VIDEO_LINK" || (a.mimeType?.startsWith("video/") ?? false);
										return (
											<div key={a.id} className="aspect-[4/3] w-full shrink-0">
												{isVideo ? (
													// eslint-disable-next-line jsx-a11y/media-has-caption
													<video src={a.url} controls className="h-full w-full object-contain" />
												) : (
													<img
														src={a.url}
														alt={feed.title}
														className="h-full w-full object-contain"
													/>
												)}
											</div>
										);
									})}
								</div>

								{mediaAttachments.length > 1 && (
									<>
										<button
											type="button"
											onClick={() => setSlideIndex((i) => Math.max(0, i - 1))}
											disabled={slideIndex === 0}
											aria-label="이전 이미지"
											className="absolute left-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-surface-container-lowest/90 text-on-surface shadow-sm disabled:opacity-40"
										>
											<Icon name="chevron_left" className="text-[20px]" />
										</button>
										<button
											type="button"
											onClick={() =>
												setSlideIndex((i) => Math.min(mediaAttachments.length - 1, i + 1))
											}
											disabled={slideIndex === mediaAttachments.length - 1}
											aria-label="다음 이미지"
											className="absolute right-2 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-full bg-surface-container-lowest/90 text-on-surface shadow-sm disabled:opacity-40"
										>
											<Icon name="chevron_right" className="text-[20px]" />
										</button>
										<div className="absolute bottom-2 left-1/2 flex -translate-x-1/2 gap-1.5">
											{mediaAttachments.map((a, i) => (
												<button
													key={a.id}
													type="button"
													onClick={() => setSlideIndex(i)}
													aria-label={`${i + 1}번째 이미지로 이동`}
													className={cn(
														"h-1.5 w-1.5 rounded-full transition-colors",
														i === slideIndex ? "bg-on-primary" : "bg-on-primary/50",
													)}
												/>
											))}
										</div>
									</>
								)}
							</div>
						)}

						{fileAttachments.length > 0 && (
							<div className="mb-4 flex flex-col gap-3">
								{fileAttachments.map((a) => (
									<a
										key={a.id}
										href={a.url}
										target="_blank"
										rel="noreferrer"
										className="flex items-center gap-2 rounded-lg border border-outline-variant p-3 text-label-md font-label-md text-on-surface hover:border-primary"
									>
										<Icon name="description" className="text-[20px] text-secondary" />
										{a.originalName ?? "첨부파일"}
									</a>
								))}
							</div>
						)}

						{likeError && <p className="mb-2 text-caption font-caption text-error">{likeError}</p>}
						<EngagementBar
							likeCount={likeCount}
							commentCount={totalCommentCount(comments)}
							liked={liked}
							onToggleLike={toggleLike}
						/>
						<div className="my-4 border-t border-outline-variant/50" />

						<h1 className="mb-2 break-words text-headline-lg font-display text-on-surface">{feed.title}</h1>
						<div className="mb-4 break-words text-body-md text-on-surface">
							{renderFormattedContent(feed.content ?? "")}
						</div>

						<Chip>{feed.category.name}</Chip>
					</>
				)}
			</div>

			<aside className="flex flex-col gap-6">
				{poll && (
					<div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-5">
						<p className="mb-3 text-label-md font-label-md text-on-surface">{poll.question}</p>
						<div className="flex flex-col gap-2">
							{poll.options.map((opt) => {
								const result = pollResults?.options.find((r) => r.optionId === opt.optionId);
								const percentage =
									pollResults && pollResults.totalWeight > 0 && result
										? Math.round((result.weight / pollResults.totalWeight) * 100)
										: 0;
								const isSelected = selectedOptionId === opt.optionId;
								return (
									<button
										key={opt.optionId}
										type="button"
										disabled={poll.closed || !pollEditMode}
										onClick={() => selectPollOption(opt.optionId)}
										className={cn(
											"relative overflow-hidden rounded border text-left transition-colors",
											isSelected ? "border-primary" : "border-outline-variant",
											(poll.closed || !pollEditMode) && "cursor-default",
										)}
									>
										<div className="h-9 bg-primary/15" style={{ width: `${percentage}%` }} />
										<div className="absolute inset-0 flex items-center justify-between px-3 text-caption font-caption text-on-surface">
											<span>{opt.optionText}</span>
											<span>{percentage}%</span>
										</div>
									</button>
								);
							})}
						</div>
						<p className="mt-3 text-caption font-caption text-secondary">
							{pollResults?.totalWeight ?? 0}표 참여 ·{" "}
							{poll.closed ? "투표 종료" : `마감 ${formatDateLabel(poll.endAt)}`}
						</p>

						{!poll.closed && (
							<div className="mt-3 flex items-center gap-3">
								{pollEditMode ? (
									<>
										<Button size="sm" onClick={submitPollVote} disabled={selectedOptionId == null || pollSubmitting}>
											{pollSubmitting ? "투표하는 중…" : "투표하기"}
										</Button>
										{poll.myVotedOptionId != null && (
											<button
												type="button"
												onClick={cancelPollEdit}
												className="text-label-md font-label-md text-secondary hover:text-on-surface"
											>
												취소
											</button>
										)}
									</>
								) : (
									<Button size="sm" variant="secondary" onClick={startPollEdit}>
										수정하기
									</Button>
								)}
							</div>
						)}

						{pollError && <p className="mt-2 text-caption font-caption text-error">{pollError}</p>}

						{isOwner && (
							<div className="mt-3 border-t border-outline-variant/50 pt-3">
								{pollManageOpen ? (
									<div className="flex flex-col gap-2">
										<input
											value={editQuestion}
											onChange={(e) => setEditQuestion(e.target.value)}
											placeholder="투표 질문"
											className="h-9 rounded border border-outline-variant bg-surface-container-low px-3 text-caption font-caption focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
										/>
										<input
											type="date"
											value={editEndAt}
											onChange={(e) => setEditEndAt(e.target.value)}
											className="h-9 rounded border border-outline-variant bg-surface-container-low px-3 text-caption font-caption focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
										/>
										<div className="flex items-center gap-3">
											<Button size="sm" onClick={submitPollEdit} disabled={pollActionSubmitting}>
												{pollActionSubmitting ? "저장하는 중…" : "저장"}
											</Button>
											<button
												type="button"
												onClick={cancelPollManage}
												className="text-caption font-caption text-secondary hover:text-on-surface"
											>
												취소
											</button>
										</div>
									</div>
								) : (
									<div className="flex items-center gap-4 text-caption font-caption text-secondary">
										{!poll.closed && (
											<>
												<button type="button" onClick={startPollManage} className="hover:text-primary">
													투표 수정
												</button>
												<button
													type="button"
													onClick={handleClosePoll}
													disabled={pollActionSubmitting}
													className="hover:text-primary"
												>
													조기 종료
												</button>
											</>
										)}
										<button
											type="button"
											onClick={handleDeletePoll}
											disabled={pollActionSubmitting}
											className="hover:text-error"
										>
											투표 삭제
										</button>
									</div>
								)}
								{pollActionError && (
									<p className="mt-2 text-caption font-caption text-error">{pollActionError}</p>
								)}
							</div>
						)}
					</div>
				)}

				<div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-5">
					<div className="mb-4 flex items-center justify-between">
						<h2 className="text-label-md font-label-md text-on-surface">댓글 {totalCommentCount(comments)}</h2>
						<span className="text-caption font-caption text-secondary">최신순</span>
					</div>

					{commentsError && (
						<p className="mb-3 text-caption font-caption text-error">{commentsError}</p>
					)}

					<div className="mb-4 flex max-h-80 flex-col gap-4 overflow-y-auto">
						{comments.map((c) => (
							<div key={c.id} className="flex flex-col gap-3">
								<div className="flex gap-3">
									<Avatar src={c.profileImageUrl ?? undefined} fallbackText={c.nickname} size={28} />
									<div className="min-w-0 flex-1">
										<span className="text-label-md font-label-md text-on-surface">{c.nickname}</span>
										{c.memberId === feed.creator.creatorId && (
											<Chip size="sm" className="ml-1.5 align-middle">
												작성자
											</Chip>
										)}
										{editingCommentId === c.id ? (
											<div className="mt-1 flex items-center gap-2">
												<input
													value={editDraft}
													onChange={(e) => setEditDraft(e.target.value)}
													onKeyDown={(e) => e.key === "Enter" && submitEditComment(c.id)}
													className="h-9 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-3 text-caption font-caption focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
												/>
												<Button size="sm" onClick={() => submitEditComment(c.id)}>
													저장
												</Button>
												<button
													type="button"
													onClick={cancelEditComment}
													className="text-caption font-caption text-secondary hover:text-on-surface"
												>
													취소
												</button>
											</div>
										) : (
											<p className="break-words text-body-md text-on-surface">{c.content}</p>
										)}
										<div className="flex items-center gap-3 text-caption font-caption text-secondary">
											<span>{formatDateLabel(c.createdAt)}</span>
											<button
												type="button"
												onClick={() => setReplyTarget((v) => (v === c.id ? null : c.id))}
												className="hover:text-primary"
											>
												답글 달기
											</button>
											{user?.memberId === c.memberId && editingCommentId !== c.id && (
												<>
													<button type="button" onClick={() => startEditComment(c)} className="hover:text-primary">
														수정
													</button>
													<button
														type="button"
														onClick={() => handleDeleteComment(c.id)}
														className="hover:text-error"
													>
														삭제
													</button>
												</>
											)}
										</div>

										{replyTarget === c.id && (
											<div className="mt-2 flex items-center gap-2">
												<input
													value={replyDraft}
													onChange={(e) => setReplyDraft(e.target.value)}
													onKeyDown={(e) => e.key === "Enter" && submitReply(c.id)}
													placeholder="답글을 입력하세요"
													className="h-9 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-3 text-caption font-caption focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
												/>
												<Button size="sm" onClick={() => submitReply(c.id)}>
													등록
												</Button>
											</div>
										)}

										{c.replies.length > 0 && (
											<div className="mt-3 flex flex-col gap-3 border-l border-outline-variant pl-4">
												{c.replies.map((r) => (
													<div key={r.id} className="flex gap-3">
														<Avatar src={r.profileImageUrl ?? undefined} fallbackText={r.nickname} size={24} />
														<div className="min-w-0 flex-1">
															<span className="text-label-md font-label-md text-on-surface">{r.nickname}</span>
															{r.memberId === feed.creator.creatorId && (
																<Chip size="sm" className="ml-1.5 align-middle">
																	작성자
																</Chip>
															)}
															{editingCommentId === r.id ? (
																<div className="mt-1 flex items-center gap-2">
																	<input
																		value={editDraft}
																		onChange={(e) => setEditDraft(e.target.value)}
																		onKeyDown={(e) => e.key === "Enter" && submitEditComment(r.id)}
																		className="h-9 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-3 text-caption font-caption focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
																	/>
																	<Button size="sm" onClick={() => submitEditComment(r.id)}>
																		저장
																	</Button>
																	<button
																		type="button"
																		onClick={cancelEditComment}
																		className="text-caption font-caption text-secondary hover:text-on-surface"
																	>
																		취소
																	</button>
																</div>
															) : (
																<p className="break-words text-body-md text-on-surface">{r.content}</p>
															)}
															<div className="flex items-center gap-3 text-caption font-caption text-secondary">
																<span>{formatDateLabel(r.createdAt)}</span>
																{user?.memberId === r.memberId && editingCommentId !== r.id && (
																	<>
																		<button
																			type="button"
																			onClick={() => startEditComment(r)}
																			className="hover:text-primary"
																		>
																			수정
																		</button>
																		<button
																			type="button"
																			onClick={() => handleDeleteComment(r.id)}
																			className="hover:text-error"
																		>
																			삭제
																		</button>
																	</>
																)}
															</div>
														</div>
													</div>
												))}
											</div>
										)}
									</div>
								</div>
							</div>
						))}
						{comments.length === 0 && (
							<p className="text-body-md text-secondary">아직 댓글이 없어요.</p>
						)}
					</div>
					<div className="flex items-center gap-2">
						<input
							value={draft}
							onChange={(e) => setDraft(e.target.value)}
							onKeyDown={(e) => e.key === "Enter" && submitComment()}
							placeholder="댓글을 입력하세요"
							className="h-10 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
						/>
						<Button size="sm" onClick={submitComment}>
							등록
						</Button>
					</div>
				</div>
			</aside>
		</div>
		<MessageDialog
			open={commentValidationMessage != null}
			message={commentValidationMessage ?? ""}
			onClose={() => setCommentValidationMessage(null)}
		/>
		<ConfirmDialog
			open={confirmDeleteCommentId != null}
			title="이 댓글을 삭제하시겠어요?"
			confirmLabel="삭제"
			onConfirm={executeDeleteComment}
			onCancel={() => setConfirmDeleteCommentId(null)}
		/>
		<ConfirmDialog
			open={confirmClosePoll}
			title="투표를 조기 종료하시겠어요?"
			description="종료 후에는 되돌릴 수 없습니다."
			confirmLabel="종료"
			onConfirm={executeClosePoll}
			onCancel={() => setConfirmClosePoll(false)}
		/>
		<ConfirmDialog
			open={confirmDeletePoll}
			title="이 투표를 삭제하시겠어요?"
			description="삭제하면 되돌릴 수 없습니다."
			confirmLabel="삭제"
			onConfirm={executeDeletePoll}
			onCancel={() => setConfirmDeletePoll(false)}
		/>
		</>
	);
}
