import { useState } from "react";
import { paths } from "@/app/paths";
import { PostCard } from "@/components/social/PostCard";
import { Avatar } from "@/components/ui/Avatar";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { findCreator } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";
import { mockPosts } from "@/mocks/posts";

const categories = ["전체", "자수", "도자기", "목공", "가죽공예", "유리공예", "캘리그라피"];

export function HomePage() {
	const [active, setActive] = useState("전체");
	const [sort, setSort] = useState<"recent" | "popular">("recent");

	const filtered = active === "전체" ? mockPosts : mockPosts.filter((p) => p.category === active);
	const items = [...filtered].sort((a, b) =>
		sort === "popular" ? b.likeCount - a.likeCount : b.id.localeCompare(a.id),
	);

	return (
		<div className="container-page py-6">
			{/* 카테고리 필터 — 헤더가 아닌 본문에 배치(겹침 버그 해소), 가로 스크롤 */}
			<div className="-mx-margin-mobile mb-5 overflow-x-auto px-margin-mobile md:mx-0 md:px-0">
				<div className="flex gap-2 whitespace-nowrap">
					{categories.map((c) => (
						<button type="button" key={c} onClick={() => setActive(c)}>
							<Chip active={active === c}>{c}</Chip>
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

			{/* 피드 그리드 */}
			<div className="grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-3">
				{items.map((post) => {
					const author = findCreator(post.creatorId);
					return (
						<PostCard
							key={post.id}
							href={paths.postDetail(post.id)}
							imageSeed={post.imageSeed}
							imageAlt={post.title}
							overlay={
								<span className="absolute left-3 top-3">
									<Chip className="bg-surface-container-lowest/90">{post.category}</Chip>
								</span>
							}
						>
							<div className="p-5">
								<h3 className="mb-1 text-headline-md font-display text-on-surface">{post.title}</h3>
								<div className="flex items-center justify-between">
									<div className="flex items-center gap-2">
										<Avatar src={mockImg(author.avatarSeed, 80, 80)} size={24} />
										<span className="text-caption font-caption text-secondary">{author.name}</span>
									</div>
									<span className="flex items-center gap-1 text-caption font-caption text-primary">
										<Icon name="favorite" className="text-[18px]" />
										{post.likeCount}
									</span>
								</div>
							</div>
						</PostCard>
					);
				})}
			</div>
		</div>
	);
}
