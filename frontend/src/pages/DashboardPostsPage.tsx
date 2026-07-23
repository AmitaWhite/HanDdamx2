import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { PostCard } from "@/components/social/PostCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { useAuth } from "@/features/auth/AuthContext";
import { deleteFeed, getMyFeeds } from "@/features/feed/feedApi";
import type { FeedSummaryResponse } from "@/features/feed/types";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";
import { formatDateLabel } from "@/lib/date";
import { VISIBILITY_BADGE_LABEL } from "@/lib/visibility";

export function DashboardPostsPage() {
	const { user } = useAuth();
	const isCreator = user?.role === "CREATOR";
	const [sort, setSort] = useState<"recent" | "popular">("recent");

	const [feeds, setFeeds] = useState<FeedSummaryResponse[]>([]);
	const [feedsLoading, setFeedsLoading] = useState(isCreator);
	const [feedsError, setFeedsError] = useState<string | null>(null);
	const [feedsPage, setFeedsPage] = useState(0);
	const [feedsHasNext, setFeedsHasNext] = useState(false);
	const [feedsLoadingMore, setFeedsLoadingMore] = useState(false);
	const [pendingDeleteIds, setPendingDeleteIds] = useState<Set<number>>(new Set());
	const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

	useEffect(() => {
		if (!isCreator) return;
		setFeedsLoading(true);
		setFeedsError(null);
		getMyFeeds(0)
			.then((res) => {
				setFeeds(res.content);
				setFeedsHasNext(res.hasNext);
				setFeedsPage(0);
			})
			.catch((err) => {
				setFeedsError(
					err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.",
				);
			})
			.finally(() => setFeedsLoading(false));
	}, [isCreator]);

	async function loadMoreFeeds() {
		if (feedsLoadingMore || !feedsHasNext) return;
		setFeedsLoadingMore(true);
		try {
			const nextPage = feedsPage + 1;
			const res = await getMyFeeds(nextPage);
			setFeeds((prev) => [...prev, ...res.content]);
			setFeedsHasNext(res.hasNext);
			setFeedsPage(nextPage);
		} catch (err) {
			setFeedsError(err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.");
		} finally {
			setFeedsLoadingMore(false);
		}
	}

	function handleDeletePost(feedId: number) {
		if (pendingDeleteIds.has(feedId)) return;
		setConfirmDeleteId(feedId);
	}

	async function executeDeletePost() {
		const feedId = confirmDeleteId;
		setConfirmDeleteId(null);
		if (feedId == null) return;
		setFeedsError(null);
		setPendingDeleteIds((prev) => new Set(prev).add(feedId));
		try {
			await deleteFeed(feedId);
			setFeeds((prev) => prev.filter((f) => f.id !== feedId));
		} catch (err) {
			setFeedsError(err instanceof ApiError ? err.message : "삭제에 실패했습니다.");
		} finally {
			setPendingDeleteIds((prev) => {
				const next = new Set(prev);
				next.delete(feedId);
				return next;
			});
		}
	}

	const sorted = [...feeds].sort((a, b) =>
		sort === "popular"
			? b.likeCount - a.likeCount
			: new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
	);
	return (
		<MyPageShell>
			<div>
				<div className="mb-8 flex flex-wrap items-center justify-between gap-4">
					<div>
						<h1 className="text-headline-lg font-display text-on-surface">게시물 관리</h1>
						<p className="mt-1 text-body-md text-secondary">작성한 게시물을 관리하세요.</p>
					</div>
					{isCreator && (
						<LinkButton to={paths.dashboardPostNew}>
							<Icon name="add" className="text-[18px]" />
							새 게시물 작성
						</LinkButton>
					)}
				</div>

				<section className="mb-12">
					<h2 className="mb-4 text-headline-md font-display text-on-surface">작성한 게시물</h2>
					{isCreator ? (
						<>
							{feedsError && <Alert className="mb-4">{feedsError}</Alert>}
							<div className="mb-4 flex items-center gap-4">
								{(["recent", "popular"] as const).map((s) => (
									<button
										key={s}
										type="button"
										onClick={() => setSort(s)}
										className={
											"text-label-md font-label-md " + (sort === s ? "font-bold text-primary" : "text-secondary")
										}
									>
										{s === "recent" ? "최신순" : "인기순"}
									</button>
								))}
							</div>
							<div className="grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-3">
								{sorted.map((post) => (
									<div key={post.id} className="relative">
										<div className="absolute right-2 top-2 z-10 flex gap-1">
											<Link
												to={paths.dashboardPostEdit(post.id)}
												aria-label="게시물 수정"
												className="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container-lowest/90 text-on-surface shadow-sm hover:bg-surface-container-low"
											>
												<Icon name="edit" className="text-[16px]" />
											</Link>
											<button
												type="button"
												aria-label="게시물 삭제"
												onClick={() => handleDeletePost(post.id)}
												disabled={pendingDeleteIds.has(post.id)}
												className="flex h-8 w-8 items-center justify-center rounded-full bg-surface-container-lowest/90 text-error shadow-sm hover:bg-surface-container-low disabled:opacity-50"
											>
												<Icon name="delete" className="text-[16px]" />
											</button>
										</div>
										<PostCard
											href={paths.postDetail(post.id)}
											imageSeed={`feed-${post.id}`}
											imageAlt={post.title}
											thumbnailUrl={post.thumbnailUrl}
											thumbnailType={post.thumbnailType}
											overlay={
												<span className="absolute left-3 top-3">
													<Chip active size="sm">
														{VISIBILITY_BADGE_LABEL[post.visibility]}
													</Chip>
												</span>
											}
										>
											<div className="p-4">
												<span className="mb-2 inline-block rounded bg-surface-container px-2 py-0.5 text-caption font-caption text-secondary">
													{post.category.name}
												</span>
												<h3 className="mb-1 truncate text-label-md font-label-md text-on-surface">{post.title}</h3>
												<p className="mb-2 truncate text-caption font-caption text-secondary">{post.content}</p>
												<div className="flex items-center justify-between text-caption font-caption text-secondary">
													<span className="flex items-center gap-3">
														<span className={cn("flex items-center gap-1", post.liked && "text-primary")}>
															<Icon name="favorite" filled={post.liked} className="text-[14px]" />
															{post.likeCount}
														</span>
														<span className="flex items-center gap-1">
															<Icon name="chat_bubble_outline" className="text-[14px]" />
															{post.commentCount}
														</span>
													</span>
													<span>{formatDateLabel(post.createdAt)}</span>
												</div>
											</div>
										</PostCard>
									</div>
								))}
							</div>
							{feedsLoading && (
								<p className="py-8 text-center text-body-md text-secondary">불러오는 중…</p>
							)}
							{!feedsLoading && sorted.length === 0 && !feedsError && (
								<div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-outline-variant py-12 text-center">
									<Icon name="draft" className="text-[32px] text-secondary" />
									<p className="text-body-md text-on-surface">아직 작성한 게시물이 없어요</p>
									<p className="text-caption font-caption text-secondary">새 게시물을 작성해보세요!</p>
								</div>
							)}
							{!feedsLoading && feedsHasNext && (
								<div className="mt-6 flex justify-center">
									<Button variant="secondary" onClick={loadMoreFeeds} disabled={feedsLoadingMore}>
										{feedsLoadingMore ? "불러오는 중…" : "더보기"}
									</Button>
								</div>
							)}
							{!feedsLoading && !feedsHasNext && sorted.length > 0 && (
								<p className="py-4 text-center text-caption font-caption text-secondary">마지막 게시물입니다</p>
							)}
						</>
					) : (
						<div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-outline-variant py-12 text-center">
							<Icon name="draft" className="text-[32px] text-secondary" />
							<p className="text-body-md text-on-surface">아직 작성한 게시물이 없어요</p>
							<p className="text-caption font-caption text-secondary">크리에이터가 되어 나만의 작품을 나누어 보세요!</p>
						</div>
					)}
				</section>
			</div>
			<ConfirmDialog
				open={confirmDeleteId != null}
				title="이 게시물을 삭제하시겠어요?"
				description="삭제하면 되돌릴 수 없습니다."
				confirmLabel="삭제"
				onConfirm={executeDeletePost}
				onCancel={() => setConfirmDeleteId(null)}
			/>
		</MyPageShell>
	);
}
