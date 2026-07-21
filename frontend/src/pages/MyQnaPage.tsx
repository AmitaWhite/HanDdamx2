import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { MyBoardCommentForm } from "@/components/member/MyBoardCommentForm";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import {
	type BoardCommentResponse,
	type BoardPostResponse,
	getBoardComments,
	getMyBoardPosts,
} from "@/features/board/boardApi";
import { useAuth } from "@/features/auth/AuthContext";
import { getCreatorProfile } from "@/features/creator/creatorApi";
import type { CreatorProfile } from "@/features/creator/types";
import { ApiError } from "@/lib/api";
import { formatRelativeTime } from "@/lib/relativeTime";

/** 새로 로드된 글들의 크리에이터 프로필과 마지막 댓글을 병렬로 가져온다. */
async function loadExtras(posts: BoardPostResponse[]) {
	const uniqueCreatorIds = [...new Set(posts.map((p) => p.creatorId))];

	const [creatorResults, commentResults] = await Promise.all([
		Promise.all(
			uniqueCreatorIds.map((id) =>
				getCreatorProfile(id).catch(() => null),
			),
		),
		Promise.all(posts.map((p) => getBoardComments(p.id).catch(() => []))),
	]);

	const creatorEntries: [number, CreatorProfile][] = [];
	uniqueCreatorIds.forEach((id, i) => {
		const profile = creatorResults[i];
		if (profile) creatorEntries.push([id, profile]);
	});

	const lastCommentEntries: [number, BoardCommentResponse | null][] = posts.map(
		(p, i) => {
			const list = commentResults[i];
			return [p.id, list.length > 0 ? list[list.length - 1] : null];
		},
	);

	return { creatorEntries, lastCommentEntries };
}

/** 마이페이지 — 내가 여러 크리에이터에게 남긴 유료 Q&A 글 모음. */
export function MyQnaPage() {
	const { user } = useAuth();
	// 크리에이터는 유료 Q&A에 질문을 작성하지 않으므로(작성자는 구독자·본인) 게시글 섹션 자체를 숨긴다.
	const isCreator = user?.role === "CREATOR";

	const [posts, setPosts] = useState<BoardPostResponse[]>([]);
	const [creators, setCreators] = useState<Map<number, CreatorProfile>>(
		new Map(),
	);
	const [lastComments, setLastComments] = useState<
		Map<number, BoardCommentResponse | null>
	>(new Map());
	const [page, setPage] = useState(0);
	const [hasNext, setHasNext] = useState(false);
	const [loading, setLoading] = useState(!isCreator);
	const [loadingMore, setLoadingMore] = useState(false);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);

	const unmountedRef = useRef(false);
	const loadingMoreRef = useRef(false);

	useEffect(() => {
		if (isCreator) return;

		let cancelled = false;
		unmountedRef.current = false;
		setLoading(true);
		setErrorMessage(null);

		getMyBoardPosts({ page: 0 })
			.then(async (pageResp) => {
				if (cancelled) return;
				const { creatorEntries, lastCommentEntries } = await loadExtras(
					pageResp.content,
				);
				if (cancelled) return;
				setPosts(pageResp.content);
				setCreators(new Map(creatorEntries));
				setLastComments(new Map(lastCommentEntries));
				setHasNext(!pageResp.last);
				setPage(0);
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setErrorMessage(
					err instanceof ApiError ? err.message : "Q&A 목록을 불러오지 못했습니다.",
				);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
			unmountedRef.current = true;
		};
	}, [isCreator]);

	async function loadMore() {
		if (loadingMoreRef.current) return;
		loadingMoreRef.current = true;
		setLoadingMore(true);
		try {
			const nextPage = page + 1;
			const pageResp = await getMyBoardPosts({ page: nextPage });
			const { creatorEntries, lastCommentEntries } = await loadExtras(
				pageResp.content,
			);
			if (unmountedRef.current) return;
			setPosts((prev) => [...prev, ...pageResp.content]);
			setCreators((prev) => new Map([...prev, ...creatorEntries]));
			setLastComments((prev) => new Map([...prev, ...lastCommentEntries]));
			setHasNext(!pageResp.last);
			setPage(nextPage);
		} catch (err) {
			if (!unmountedRef.current) {
				setErrorMessage(
					err instanceof ApiError ? err.message : "Q&A 목록을 불러오지 못했습니다.",
				);
			}
		} finally {
			loadingMoreRef.current = false;
			if (!unmountedRef.current) setLoadingMore(false);
		}
	}

	return (
		<MyPageShell>
			<div>
				<div className="mb-8">
					<h1 className="text-headline-lg font-display text-on-surface">
						작가와의 Q&A
					</h1>
					<p className="mt-1 text-body-md text-secondary">
						내가 남긴 유료 Q&A 글을 모아봤어요.
					</p>
				</div>

				{errorMessage && !isCreator && (
					<div className="mb-4 rounded-xl border border-primary/30 bg-primary/5 p-4 text-body-md text-primary">
						{errorMessage}
					</div>
				)}

				{!isCreator && (
					<>
						<section>
							<h2 className="mb-4 text-headline-md font-display text-on-surface">
								Q&A 게시글 목록
							</h2>

							<div className="min-h-[220px]">
								{loading && (
									<p className="py-12 text-center text-body-md text-secondary">
										불러오는 중…
									</p>
								)}

								{!loading && posts.length === 0 && !errorMessage && (
									<div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-outline-variant py-12 text-center">
										<Icon name="forum" className="text-[32px] text-secondary" />
										<p className="text-body-md text-on-surface">아직 남긴 Q&A가 없어요</p>
										<p className="text-caption font-caption text-secondary">
											구독 중인 작가의 Q&A 게시판에서 질문을 남겨보세요!
										</p>
									</div>
								)}

								{!loading && posts.length > 0 && (
									<div className="flex flex-col gap-3">
										{posts.map((post) => {
											const creator = creators.get(post.creatorId);
											const lastComment = lastComments.get(post.id);
											return (
												<Link key={post.id} to={paths.qnaPost(post.id)}>
													<Card interactive className="p-4">
														<div className="flex items-start gap-3">
															<Avatar
																src={creator?.profileImageUrl ?? undefined}
																alt={creator?.nickname}
																size={40}
															/>
															<div className="min-w-0 flex-1">
																<div className="flex flex-wrap items-center gap-2">
																	<span className="text-label-md font-label-md text-on-surface">
																		{creator?.nickname ?? `크리에이터 #${post.creatorId}`}
																	</span>
																	<span className="text-caption font-caption text-secondary">
																		· {formatRelativeTime(post.createdAt)}
																	</span>
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
																</div>
																<p className="mt-1 truncate text-body-md font-bold text-on-surface">
																	{post.title}
																</p>
																<p className="mt-0.5 line-clamp-2 text-body-md text-secondary">
																	{post.content}
																</p>
																{lastComment && (
																	<p className="mt-2 truncate text-caption font-caption text-secondary">
																		💬 {lastComment.content}
																	</p>
																)}
															</div>
														</div>
													</Card>
												</Link>
											);
										})}
									</div>
								)}
							</div>

							{!loading && hasNext && (
								<div className="mt-6 text-center">
									<Button variant="secondary" onClick={loadMore} disabled={loadingMore}>
										{loadingMore ? "불러오는 중…" : "더보기"}
									</Button>
								</div>
							)}
						</section>

						<hr className="my-10 border-outline-variant/50" />
					</>
				)}

				<MyBoardCommentForm />
			</div>
		</MyPageShell>
	);
}
