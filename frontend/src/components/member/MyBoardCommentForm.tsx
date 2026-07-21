import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { getMyBoardComments } from "@/features/member/memberApi";
import type { MyBoardCommentResponse } from "@/features/member/types";
import { ApiError } from "@/lib/api";
import { formatRelativeTime } from "@/lib/relativeTime";

export function MyBoardCommentForm() {
	const [comments, setComments] = useState<MyBoardCommentResponse[]>([]);
	const [page, setPage] = useState(0);
	const [hasNext, setHasNext] = useState(false);
	const [loading, setLoading] = useState(true);
	const [loadingMore, setLoadingMore] = useState(false);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);

	const unmountedRef = useRef(false);
	const loadingMoreRef = useRef(false);

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		setErrorMessage(null);

		getMyBoardComments(0)
			.then((slice) => {
				if (cancelled) return;
				setComments(slice.content);
				setHasNext(slice.hasNext);
				setPage(0);
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setErrorMessage(
					err instanceof ApiError ? err.message : "댓글 목록을 불러오지 못했습니다.",
				);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, []);

	useEffect(() => {
		unmountedRef.current = false; // TODO : Strict Mode 해제 후 true로 변경 필요
		return () => {
			unmountedRef.current = true;
		};
	}, []);

	async function loadMore() {
		if (loadingMoreRef.current) return;
		loadingMoreRef.current = true;
		setLoadingMore(true);
		try {
			const nextPage = page + 1;
			const slice = await getMyBoardComments(nextPage);
			if (unmountedRef.current) return;
			setComments((prev) => [...prev, ...slice.content]);
			setHasNext(slice.hasNext);
			setPage(nextPage);
		} catch (err) {
			if (!unmountedRef.current) {
				setErrorMessage(
					err instanceof ApiError ? err.message : "댓글 목록을 불러오지 못했습니다.",
				);
			}
		} finally {
			loadingMoreRef.current = false;
			if (!unmountedRef.current) setLoadingMore(false);
		}
	}

	const hasComments = !loading && comments.length > 0;

	return (
		<section>
			<h2 className="mb-4 text-headline-md font-display text-on-surface">
				Q&A 댓글 목록
			</h2>

			{errorMessage && (
				<div className="mb-4 rounded-xl border border-primary/30 bg-primary/5 p-4 text-body-md text-primary">
					{errorMessage}
				</div>
			)}

			<div className="min-h-[180px]">
				{loading && (
					<p className="py-8 text-center text-body-md text-secondary">불러오는 중…</p>
				)}

				{!loading && comments.length === 0 && !errorMessage && (
					<p className="py-8 text-center text-body-md text-secondary">
						아직 남긴 댓글이 없어요.
					</p>
				)}

				{hasComments && (
					<div className="flex flex-col gap-3">
						{comments.map((c) => (
							<Card key={c.commentId} className="p-5">
								<p className="mb-2 text-body-md text-on-surface">&ldquo;{c.content}&rdquo;</p>
								<div className="flex items-center justify-between text-caption font-caption text-secondary">
									<span>{formatRelativeTime(c.createdAt)}</span>
									<Link to={paths.qnaPost(c.boardPostId)} className="text-primary hover:underline">
										원문 보기
									</Link>
								</div>
							</Card>
						))}
					</div>
				)}
			</div>

			{hasComments && (
				<div className="mt-6 pb-4 text-center">
					{hasNext ? (
						<Button variant="secondary" onClick={loadMore} disabled={loadingMore}>
							{loadingMore ? "불러오는 중…" : "더보기"}
						</Button>
					) : (
						<p className="text-body-md text-secondary">모든 댓글을 확인했어요.</p>
					)}
				</div>
			)}
		</section>
	);
}