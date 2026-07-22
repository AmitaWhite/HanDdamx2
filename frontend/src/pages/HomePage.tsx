import { useEffect, useRef, useState } from "react";
import { paths } from "@/app/paths";
import { PostCard } from "@/components/social/PostCard";
import { Avatar } from "@/components/ui/Avatar";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { getActiveCategories } from "@/features/category/categoryApi";
import type { CategoryResponse } from "@/features/category/types";
import { getExploreFeeds } from "@/features/feed/feedApi";
import type { FeedSummaryResponse } from "@/features/feed/types";
import { ApiError } from "@/lib/api";
import { useInfiniteScroll } from "@/lib/useInfiniteScroll";
import { mockImg } from "@/mocks/helpers";

export function HomePage() {
	const [categories, setCategories] = useState<CategoryResponse[]>([]);
	const [activeCategoryId, setActiveCategoryId] = useState<number | undefined>(undefined);
	const [sort, setSort] = useState<"recent" | "popular">("recent");

	const [feeds, setFeeds] = useState<FeedSummaryResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [page, setPage] = useState(0);
	const [hasNext, setHasNext] = useState(false);
	const [loadingMore, setLoadingMore] = useState(false);
	const loadMoreInFlightRef = useRef(false);

	useEffect(() => {
		getActiveCategories()
			.then(setCategories)
			.catch(() => {
				// 카테고리 필터는 부가 기능이라 실패해도 "전체" 탐색은 계속 가능하게 둔다.
			});
	}, []);

	useEffect(() => {
		setLoading(true);
		setError(null);
		getExploreFeeds(activeCategoryId)
			.then((res) => {
				setFeeds(res.content);
				setHasNext(res.hasNext);
				setPage(0);
			})
			.catch((err) => {
				setError(err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.");
			})
			.finally(() => setLoading(false));
	}, [activeCategoryId]);

	async function loadMore() {
		if (loadMoreInFlightRef.current || !hasNext) return;
		loadMoreInFlightRef.current = true;
		setLoadingMore(true);
		try {
			const nextPage = page + 1;
			const res = await getExploreFeeds(activeCategoryId, nextPage);
			setFeeds((prev) => [...prev, ...res.content]);
			setHasNext(res.hasNext);
			setPage(nextPage);
		} catch (err) {
			setError(err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.");
		} finally {
			loadMoreInFlightRef.current = false;
			setLoadingMore(false);
		}
	}

	const sentinelRef = useInfiniteScroll(loadMore, hasNext);

	const sorted = [...feeds].sort((a, b) =>
		sort === "popular"
			? b.likeCount - a.likeCount
			: new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
	);

	return (
		<div className="container-page py-6">
			{/* 카테고리 필터 — 헤더가 아닌 본문에 배치(겹침 버그 해소), 가로 스크롤 */}
			<div className="-mx-margin-mobile mb-5 overflow-x-auto px-margin-mobile md:mx-0 md:px-0">
				<div className="flex gap-2 whitespace-nowrap">
					<button type="button" onClick={() => setActiveCategoryId(undefined)}>
						<Chip active={activeCategoryId === undefined}>전체</Chip>
					</button>
					{categories.map((c) => (
						<button type="button" key={c.categoryId} onClick={() => setActiveCategoryId(c.categoryId)}>
							<Chip active={activeCategoryId === c.categoryId}>{c.name}</Chip>
						</button>
					))}
				</div>
			</div>

			{/* 정렬 탭 */}
			<div className="mb-6 flex items-center gap-4">
				{(["recent", "popular"] as const).map((s) => (
					<button
						type="button"
						key={s}
						onClick={() => setSort(s)}
						className={
							"text-label-md font-label-md " + (sort === s ? "font-bold text-primary" : "text-secondary")
						}
					>
						{s === "recent" ? "최신순" : "인기순"}
					</button>
				))}
			</div>

			{error && <p className="py-8 text-center text-body-md text-secondary">{error}</p>}

			{/* 피드 그리드 */}
			<div className="grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-3">
				{sorted.map((post) => (
					<PostCard
						key={post.id}
						href={paths.postDetail(post.id)}
						imageSeed={`feed-${post.id}`}
						imageAlt={post.title}
						overlay={
							<span className="absolute left-3 top-3">
								<Chip className="bg-surface-container-lowest/90">{post.category.name}</Chip>
							</span>
						}
					>
						<div className="p-5">
							<h3 className="mb-1 text-headline-md font-display text-on-surface">{post.title}</h3>
							<div className="flex items-center justify-between">
								<div className="flex items-center gap-2">
									<Avatar
										src={
											post.creator.profileImageUrl ??
											mockImg(`creator-${post.creator.creatorId}`, 80, 80)
										}
										size={24}
									/>
									<span className="text-caption font-caption text-secondary">
										{post.creator.nickname}
									</span>
								</div>
								<span className="flex items-center gap-1 text-caption font-caption text-primary">
									<Icon name="favorite" className="text-[18px]" />
									{post.likeCount}
								</span>
							</div>
						</div>
					</PostCard>
				))}
			</div>

			{loading && <p className="py-8 text-center text-body-md text-secondary">불러오는 중…</p>}
			{!loading && !error && sorted.length === 0 && (
				<p className="py-8 text-center text-body-md text-secondary">게시물이 없어요.</p>
			)}
			{hasNext && <div ref={sentinelRef} className="h-1" />}
			{loadingMore && (
				<p className="py-4 text-center text-body-md text-secondary">불러오는 중…</p>
			)}
			{!loading && !hasNext && sorted.length > 0 && (
				<p className="py-4 text-center text-caption font-caption text-secondary">마지막 게시물입니다</p>
			)}
		</div>
	);
}
