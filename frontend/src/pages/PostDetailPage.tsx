import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { EngagementBar } from "@/components/social/EngagementBar";
import { Icon } from "@/components/ui/Icon";
import {
	createFeedComment,
	createFeedCommentReply,
	getFeedComments,
} from "@/features/comment/commentApi";
import type { FeedCommentResponse } from "@/features/comment/types";
import { getFeed } from "@/features/feed/feedApi";
import type { FeedDetailResponse } from "@/features/feed/types";
import { likeFeed, unlikeFeed } from "@/features/like/likeApi";
import { changePollVote, getPoll, getPollResults, votePoll } from "@/features/poll/pollApi";
import type { PollResponse, PollResultResponse } from "@/features/poll/types";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";
import { mockImg } from "@/mocks/helpers";

const REQUIRED_LEVEL_LABEL: Record<string, string> = {
	FREE_SUBSCRIBER: "무료 구독자",
	PAID_SUBSCRIBER: "유료 구독자",
};

function formatDateLabel(iso: string): string {
	const d = new Date(iso);
	return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
}

function totalCommentCount(comments: FeedCommentResponse[]): number {
	return comments.reduce((sum, c) => sum + 1 + c.replies.length, 0);
}

export function PostDetailPage() {
	const { postId = "" } = useParams();

	const [feed, setFeed] = useState<FeedDetailResponse | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	const [likeCount, setLikeCount] = useState(0);
	const [liked, setLiked] = useState(false);
	const [likeError, setLikeError] = useState<string | null>(null);

	const [comments, setComments] = useState<FeedCommentResponse[]>([]);
	const [commentsError, setCommentsError] = useState<string | null>(null);
	const [draft, setDraft] = useState("");
	const [replyTarget, setReplyTarget] = useState<number | null>(null);
	const [replyDraft, setReplyDraft] = useState("");

	const [poll, setPoll] = useState<PollResponse | null>(null);
	const [pollResults, setPollResults] = useState<PollResultResponse | null>(null);
	const [pollError, setPollError] = useState<string | null>(null);

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
		if (!feed?.pollId) return;
		const pollId = feed.pollId;
		Promise.all([getPoll(pollId), getPollResults(pollId)])
			.then(([p, r]) => {
				setPoll(p);
				setPollResults(r);
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
		if (!feed || !draft.trim()) return;
		try {
			await createFeedComment(feed.id, draft.trim());
			setDraft("");
			await refreshComments();
		} catch (err) {
			setCommentsError(err instanceof ApiError ? err.message : "댓글 등록에 실패했습니다.");
		}
	}

	async function submitReply(parentCommentId: number) {
		if (!feed || !replyDraft.trim()) return;
		try {
			await createFeedCommentReply(feed.id, parentCommentId, replyDraft.trim());
			setReplyDraft("");
			setReplyTarget(null);
			await refreshComments();
		} catch (err) {
			setCommentsError(err instanceof ApiError ? err.message : "답글 등록에 실패했습니다.");
		}
	}

	async function handleVote(optionId: number) {
		if (!poll || poll.closed) return;
		if (poll.myVotedOptionId === optionId) return;
		setPollError(null);
		try {
			if (poll.myVotedOptionId == null) {
				await votePoll(poll.pollId, optionId);
			} else {
				await changePollVote(poll.pollId, optionId);
			}
			const [freshPoll, freshResults] = await Promise.all([
				getPoll(poll.pollId),
				getPollResults(poll.pollId),
			]);
			setPoll(freshPoll);
			setPollResults(freshResults);
		} catch (err) {
			setPollError(err instanceof ApiError ? err.message : "투표에 실패했습니다.");
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

	return (
		<div className="container-page grid grid-cols-1 gap-gutter py-6 lg:grid-cols-[1fr_400px]">
			<div>
				<Link
					to={paths.home}
					className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
				>
					<Icon name="arrow_back" className="text-[18px]" />
					돌아가기
				</Link>

				<div className="mb-4 flex items-center gap-3">
					<Link to={paths.creator(feed.creator.creatorId)}>
						<Avatar src={mockImg(feed.creator.profileImageUrl ?? `creator-${feed.creator.creatorId}`, 80, 80)} size={40} />
					</Link>
					<div className="min-w-0">
						<Link
							to={paths.creator(feed.creator.creatorId)}
							className="text-label-md font-label-md text-on-surface hover:text-primary"
						>
							{feed.creator.nickname}
						</Link>
						<p className="text-caption font-caption text-secondary">{feed.category.name}</p>
					</div>
					{feed.visibility !== "PUBLIC" && (
						<Chip active size="sm" className="ml-auto shrink-0">
							{feed.visibility === "PAID_SUBSCRIBER" ? "유료 구독자 공개" : "무료 구독자 공개"}
						</Chip>
					)}
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
						<div className="mb-4 aspect-[4/3] overflow-hidden rounded-xl">
							<img
								src={mockImg(`feed-${feed.id}`, 900, 700)}
								alt={feed.title}
								className="h-full w-full object-cover"
							/>
						</div>

						{likeError && <p className="mb-2 text-caption font-caption text-error">{likeError}</p>}
						<EngagementBar
							likeCount={likeCount}
							commentCount={totalCommentCount(comments)}
							liked={liked}
							onToggleLike={toggleLike}
							className="mb-4"
						/>

						<h1 className="mb-2 text-headline-lg font-display text-on-surface">{feed.title}</h1>
						<p className="text-body-md text-on-surface">{feed.content}</p>
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
								const isMine = poll.myVotedOptionId === opt.optionId;
								return (
									<button
										key={opt.optionId}
										type="button"
										disabled={poll.closed}
										onClick={() => handleVote(opt.optionId)}
										className={cn(
											"relative overflow-hidden rounded border text-left transition-colors",
											isMine ? "border-primary" : "border-outline-variant",
											poll.closed && "cursor-default",
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
							{pollResults?.totalWeight ?? 0}표 참여{poll.closed ? " · 투표 종료" : ""}
						</p>
						{pollError && <p className="mt-2 text-caption font-caption text-error">{pollError}</p>}
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
									<Avatar src={mockImg(`member-${c.memberId}`, 80, 80)} size={28} />
									<div className="min-w-0 flex-1">
										<span className="text-label-md font-label-md text-on-surface">{c.nickname}</span>
										<p className="text-body-md text-on-surface">{c.content}</p>
										<div className="flex items-center gap-3 text-caption font-caption text-secondary">
											<span>{formatDateLabel(c.createdAt)}</span>
											<button
												type="button"
												onClick={() => setReplyTarget((v) => (v === c.id ? null : c.id))}
												className="hover:text-primary"
											>
												답글 달기
											</button>
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
														<Avatar src={mockImg(`member-${r.memberId}`, 80, 80)} size={24} />
														<div>
															<span className="text-label-md font-label-md text-on-surface">{r.nickname}</span>
															<p className="text-body-md text-on-surface">{r.content}</p>
															<p className="text-caption font-caption text-secondary">{formatDateLabel(r.createdAt)}</p>
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
	);
}
