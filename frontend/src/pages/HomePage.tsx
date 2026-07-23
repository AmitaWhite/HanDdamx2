import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { PostCard } from "@/components/social/PostCard";
import { Avatar } from "@/components/ui/Avatar";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { getActiveCategories } from "@/features/category/categoryApi";
import type { CategoryResponse } from "@/features/category/types";
import { getExploreFeeds } from "@/features/feed/feedApi";
import type { FeedSummaryResponse } from "@/features/feed/types";
import { searchCreators, searchFeeds } from "@/features/search/searchApi";
import type { CreatorSearchResponse } from "@/features/search/types";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";
import { truncateText } from "@/lib/text";
import { useInfiniteScroll } from "@/lib/useInfiniteScroll";
import { VISIBILITY_BADGE_LABEL } from "@/lib/visibility";

export function HomePage() {
	// 헤더 검색창에서 넘어온 keyword. 없으면 기존 둘러보기 동작.
	const [searchParams] = useSearchParams();
	const keyword = (searchParams.get("q") ?? "").trim();

	const [categories, setCategories] = useState<CategoryResponse[]>([]);
	const [activeCategoryId, setActiveCategoryId] = useState<number | undefined>(undefined);
	const [sort, setSort] = useState<"recent" | "popular">("recent");

	const [feeds, setFeeds] = useState<FeedSummaryResponse[]>([]);
	const [creators, setCreators] = useState<CreatorSearchResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [page, setPage] = useState(0);
	const [hasNext, setHasNext] = useState(false);
	const [loadingMore, setLoadingMore] = useState(false);
	const loadMoreInFlightRef = useRef(false);

	// keyword 유무로 검색 API / 탐색 API 분기.
	const fetchFeeds = (nextPage: number) =>
		keyword
			? searchFeeds(keyword, activeCategoryId, nextPage)
			: getExploreFeeds(activeCategoryId, nextPage);

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
		fetchFeeds(0)
			.then((res) => {
				setFeeds(res.content);
				setHasNext(res.hasNext);
				setPage(0);
			})
			.catch((err) => {
				setError(err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.");
			})
			.finally(() => setLoading(false));
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [activeCategoryId, keyword]);

	// 크리에이터 매칭 결과 — keyword 있을 때만 조회.
	useEffect(() => {
		if (!keyword) {
			setCreators([]);
			return;
		}
		searchCreators(keyword)
			.then((res) => setCreators(res.content))
			.catch(() => setCreators([])); // 부가 섹션이라 실패해도 피드 검색은 유지.
	}, [keyword]);

	async function loadMore() {
		if (loadMoreInFlightRef.current || !hasNext) return;
		loadMoreInFlightRef.current = true;
		setLoadingMore(true);
		try {
			const nextPage = page + 1;
			const res = await fetchFeeds(nextPage);
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

			{/* 크리에이터 매칭 섹션 — 검색 중이고 결과가 있을 때만 */}
			{keyword && creators.length > 0 && (
				<section className="mb-8">
					<h2 className="mb-3 text-headline-md text-on-surface">크리에이터</h2>
					<div className="-mx-margin-mobile flex gap-4 overflow-x-auto px-margin-mobile md:mx-0 md:px-0">
						{creators.map((c) => (
							<Link
								key={c.creatorId}
								to={paths.creator(c.creatorId)}
								className="flex w-20 shrink-0 flex-col items-center gap-2"
							>
								<Avatar src={c.profileImageUrl ?? undefined} fallbackText={c.nickname} size={56} />
								<span className="w-full truncate text-center text-caption font-caption text-on-surface">
									{c.nickname}
								</span>
							</Link>
						))}
					</div>
				</section>
			)}

			{/* 작품(피드) 섹션 제목 — 검색 중일 때만 노출해 크리에이터 섹션과 구분 */}
			{keyword && (
				<h2 className="mb-3 text-headline-md text-on-surface">작품</h2>
			)}

			{/* 피드 그리드 */}
			<div className="grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-3">
				{sorted.map((post) => (
					<PostCard
						key={post.id}
						href={paths.postDetail(post.id)}
						imageSeed={`feed-${post.id}`}
						imageAlt={post.title}
						thumbnailUrl={post.thumbnailUrl}
						thumbnailType={post.thumbnailType}
						overlay={
							<>
								<span className="absolute left-3 top-3">
									<Chip className="bg-surface-container-lowest/90">{post.category.name}</Chip>
								</span>
								<span className="absolute right-3 top-3">
									<Chip active size="sm">
										{VISIBILITY_BADGE_LABEL[post.visibility]}
									</Chip>
								</span>
							</>
						}
					>
						<div className="p-5">
							<h3 className="mb-1 text-headline-md font-display text-on-surface">
								{truncateText(post.title, 13)}
							</h3>
							<div className="flex items-center justify-between">
								<div className="flex items-center gap-2">
									<Avatar
										src={post.creator.profileImageUrl ?? undefined}
										fallbackText={post.creator.nickname}
										size={24}
									/>
									<span className="text-caption font-caption text-secondary">
										{post.creator.nickname}
									</span>
								</div>
								<span className="flex items-center gap-3 text-caption font-caption text-secondary">
									<span className={cn("flex items-center gap-1", post.liked && "text-primary")}>
										<Icon name="favorite" filled={post.liked} className="text-[18px]" />
										{post.likeCount}
									</span>
									<span className="flex items-center gap-1">
										<Icon name="chat_bubble_outline" className="text-[18px]" />
										{post.commentCount}
									</span>
								</span>
							</div>
						</div>
					</PostCard>
				))}
			</div>

			{loading && <p className="py-8 text-center text-body-md text-secondary">불러오는 중…</p>}
			{!loading && !error && sorted.length === 0 && (
				<p className="py-8 text-center text-body-md text-secondary">
					{keyword
						? `‘${keyword}’에 대한 작품이 없어요.`
						: "게시물이 없어요."}
				</p>
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
