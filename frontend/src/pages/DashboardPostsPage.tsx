import { useState } from "react";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { PostCard } from "@/components/social/PostCard";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { useAuth } from "@/features/auth/AuthContext";
import { mockPosts } from "@/mocks/posts";

/** 로그인 계정과 mock 크리에이터를 잇는 백엔드 매핑이 아직 없어 임시로 고정한 값. */
const MOCK_CREATOR_ID = "suyeon";

export function DashboardPostsPage() {
	const { user } = useAuth();
	const isCreator = user?.role === "CREATOR";
	const [sort, setSort] = useState<"recent" | "popular">("recent");
	const myPosts = mockPosts.filter((p) => p.creatorId === MOCK_CREATOR_ID);
	const sorted = [...myPosts].sort((a, b) =>
		sort === "popular" ? b.likeCount - a.likeCount : b.id.localeCompare(a.id),
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
									<PostCard key={post.id} href={paths.postDetail(post.id)} imageSeed={post.imageSeed} imageAlt={post.title}>
										<div className="p-4">
											<span className="mb-2 inline-block rounded bg-surface-container px-2 py-0.5 text-caption font-caption text-secondary">
												{post.category}
											</span>
											<h3 className="mb-1 truncate text-label-md font-label-md text-on-surface">{post.title}</h3>
											<p className="mb-2 truncate text-caption font-caption text-secondary">{post.excerpt}</p>
											<div className="flex items-center justify-between text-caption font-caption text-secondary">
												<span className="flex items-center gap-3">
													<span className="flex items-center gap-1">
														<Icon name="favorite" className="text-[14px]" />
														{post.likeCount}
													</span>
													<span className="flex items-center gap-1">
														<Icon name="chat_bubble_outline" className="text-[14px]" />
														{post.commentCount}
													</span>
												</span>
												<span>{post.createdAtLabel}</span>
											</div>
										</div>
									</PostCard>
								))}
							</div>
							{sorted.length === 0 && (
								<p className="py-8 text-center text-body-md text-secondary">작성한 게시물이 없어요.</p>
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
		</MyPageShell>
	);
}
