import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { getHomeFeed } from "@/features/feed/feedApi";
import type { FeedSummaryResponse } from "@/features/feed/types";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";
import { formatDateLabel } from "@/lib/date";
import { useInfiniteScroll } from "@/lib/useInfiniteScroll";
import { VISIBILITY_BADGE_LABEL } from "@/lib/visibility";
import { mockCreators } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";

export function SubscribeFeedPage() {
	// 사이드바 "구독 중인 작가"는 별도 구독 목록 API 영역 — 여기서는 그대로 mock 유지
	const subscribedCreators = mockCreators.slice(0, 5);

	const [feeds, setFeeds] = useState<FeedSummaryResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [page, setPage] = useState(0);
	const [hasNext, setHasNext] = useState(false);
	const [loadingMore, setLoadingMore] = useState(false);
	const loadMoreInFlightRef = useRef(false);

	useEffect(() => {
		getHomeFeed()
			.then((res) => {
				setFeeds(res.content);
				setHasNext(res.hasNext);
				setPage(0);
			})
			.catch((err) => {
				setError(err instanceof ApiError ? err.message : "피드를 불러오지 못했습니다.");
			})
			.finally(() => setLoading(false));
	}, []);

	async function loadMore() {
		if (loadMoreInFlightRef.current || !hasNext) return;
		loadMoreInFlightRef.current = true;
		setLoadingMore(true);
		try {
			const nextPage = page + 1;
			const res = await getHomeFeed(undefined, nextPage);
			setFeeds((prev) => [...prev, ...res.content]);
			setHasNext(res.hasNext);
			setPage(nextPage);
		} catch (err) {
			setError(err instanceof ApiError ? err.message : "피드를 불러오지 못했습니다.");
		} finally {
			loadMoreInFlightRef.current = false;
			setLoadingMore(false);
		}
	}

	const sentinelRef = useInfiniteScroll(loadMore, hasNext);

	return (
		<div className="container-page grid grid-cols-1 gap-gutter py-6 lg:grid-cols-[1fr_280px]">
			<div className="min-w-0">
				<h1 className="mb-6 text-headline-lg font-display text-on-surface">구독 중인 작가의 새 게시물</h1>

				{error && <p className="py-8 text-center text-body-md text-secondary">{error}</p>}

				<div className="flex flex-col gap-gutter">
					{feeds.map((post) => (
						<Link key={post.id} to={paths.postDetail(post.id)} className="min-w-0">
							<Card interactive className="overflow-hidden">
								<div className="flex items-center gap-3 p-5 pb-4">
									<Avatar
										src={post.creator.profileImageUrl ?? undefined}
										fallbackText={post.creator.nickname}
										size={40}
									/>
									<div className="min-w-0">
										<p className="truncate text-label-md font-label-md text-on-surface">
											{post.creator.nickname}
										</p>
										<p className="truncate text-caption font-caption text-secondary">
											{post.category.name}
										</p>
									</div>
									<Chip active size="sm" className="ml-auto shrink-0">
										{VISIBILITY_BADGE_LABEL[post.visibility]}
									</Chip>
								</div>
								<div className="aspect-[16/9] overflow-hidden">
									{post.thumbnailUrl && post.thumbnailType === "VIDEO" ? (
										<video
											src={post.thumbnailUrl}
											muted
											playsInline
											preload="metadata"
											className="h-full w-full object-cover"
										/>
									) : (
										<img
											src={post.thumbnailUrl ?? mockImg(`feed-${post.id}`, 900, 500)}
											alt={post.title}
											className="h-full w-full object-cover"
										/>
									)}
								</div>
								<div className="p-5">
									<div className="mb-2 flex items-center gap-4 text-caption font-caption text-secondary">
										<span
											className={cn(
												"flex items-center gap-1",
												post.liked && "text-primary",
											)}
										>
											<Icon name="favorite" filled={post.liked} className="text-[16px]" />
											{post.likeCount}
										</span>
										<span className="flex items-center gap-1">
											<Icon name="chat_bubble_outline" className="text-[16px]" />
											{post.commentCount}
										</span>
									</div>
									<p className="mb-1 truncate text-headline-md font-display text-on-surface">{post.title}</p>
									<p className="mb-1 line-clamp-2 text-body-md text-on-surface">{post.content}</p>
									<p className="text-caption font-caption text-secondary">
										{formatDateLabel(post.createdAt)}
									</p>
								</div>
							</Card>
						</Link>
					))}
				</div>

				{loading && <p className="py-8 text-center text-body-md text-secondary">불러오는 중…</p>}
				{!loading && !error && feeds.length === 0 && (
					<p className="py-8 text-center text-body-md text-secondary">
						구독 중인 작가의 새 게시물이 없어요.
					</p>
				)}
				{hasNext && <div ref={sentinelRef} className="h-1" />}
				{loadingMore && (
					<p className="py-4 text-center text-body-md text-secondary">불러오는 중…</p>
				)}
				{!loading && !hasNext && feeds.length > 0 && (
					<p className="py-4 text-center text-caption font-caption text-secondary">마지막 게시물입니다</p>
				)}
			</div>

			<aside className="hidden lg:block">
				<div className="sticky top-[96px] rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-5">
					<h2 className="mb-4 text-label-md font-label-md text-on-surface">구독 중인 작가</h2>
					<div className="flex flex-col gap-3">
						{subscribedCreators.map((creator) => (
							<Link key={creator.id} to={paths.creator(creator.id)} className="flex items-center gap-3">
								<Avatar fallbackText={creator.name} size={36} />
								<div className="min-w-0">
									<p className="truncate text-label-md font-label-md text-on-surface">{creator.name}</p>
									<p className="truncate text-caption font-caption text-secondary">{creator.category}</p>
								</div>
							</Link>
						))}
					</div>
					<Link to={paths.home} className="mt-4 block text-label-md font-label-md text-primary hover:underline">
						더 많은 작가 둘러보기
					</Link>
				</div>
			</aside>
		</div>
	);
}
