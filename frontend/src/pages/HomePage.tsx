import { useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";

const img = (seed: string) => `https://picsum.photos/seed/${seed}/700/700`;

const categories = [
	"전체",
	"자수",
	"도자기",
	"목공",
	"가죽공예",
	"유리공예",
	"캘리그라피",
];

const feed = [
	{
		id: 1,
		author: "이수연",
		title: "들꽃 리스, 마지막 스티치 배색 고민",
		tag: "자수",
		likes: 128,
		seed: "feed1",
	},
	{
		id: 2,
		author: "목공소년",
		title: "월넛 트레이 샌딩 작업 중",
		tag: "목공",
		likes: 51,
		seed: "feed2",
	},
	{
		id: 3,
		author: "조리본",
		title: "리본자수 파우치 완성",
		tag: "자수",
		likes: 42,
		seed: "feed3",
	},
	{
		id: 4,
		author: "김도예",
		title: "백자 달항아리 초벌 전 정리",
		tag: "도자기",
		likes: 88,
		seed: "feed4",
	},
	{
		id: 5,
		author: "정가죽",
		title: "카드지갑 코바 마감",
		tag: "가죽공예",
		likes: 63,
		seed: "feed5",
	},
	{
		id: 6,
		author: "윤유리",
		title: "유리병 색 배합 실험",
		tag: "유리공예",
		likes: 37,
		seed: "feed6",
	},
];

export function HomePage() {
	const [active, setActive] = useState("전체");
	const [sort, setSort] = useState<"recent" | "popular">("recent");

	const items = active === "전체" ? feed : feed.filter((f) => f.tag === active);

	return (
		<div className="container-page py-6">
			{/* 카테고리 필터 — 헤더가 아닌 본문에 배치(겹침 버그 해소), 가로 스크롤 */}
			<div className="-mx-margin-mobile mb-5 overflow-x-auto px-margin-mobile md:mx-0 md:px-0">
				<div className="flex gap-2 whitespace-nowrap">
					{categories.map((c) => (
						<button key={c} onClick={() => setActive(c)}>
							<Chip active={active === c}>{c}</Chip>
						</button>
					))}
				</div>
			</div>

			{/* 정렬 탭 */}
			<div className="mb-6 flex items-center gap-4">
				{(["recent", "popular"] as const).map((s) => (
					<button
						key={s}
						onClick={() => setSort(s)}
						className={
							"text-label-md font-label-md " +
							(sort === s ? "font-bold text-primary" : "text-secondary")
						}
					>
						{s === "recent" ? "최신순" : "인기순"}
					</button>
				))}
			</div>

			{/* 피드 그리드 */}
			<div className="grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-3">
				{items.map((item) => (
					<Link key={item.id} to={paths.postDetail(item.id)}>
						<Card interactive className="group overflow-hidden">
							<div className="relative aspect-square overflow-hidden">
								<span className="absolute left-3 top-3 z-10">
									<Chip className="bg-surface-container-lowest/90">
										{item.tag}
									</Chip>
								</span>
								<img
									src={img(item.seed)}
									alt={item.title}
									className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
								/>
							</div>
							<div className="p-5">
								<h3 className="mb-1 text-headline-md font-display text-on-surface">
									{item.title}
								</h3>
								<div className="flex items-center justify-between">
									<div className="flex items-center gap-2">
										<Avatar size={24} />
										<span className="text-caption font-caption text-secondary">
											{item.author}
										</span>
									</div>
									<span className="flex items-center gap-1 text-caption font-caption text-primary">
										<Icon name="favorite" className="text-[18px]" />
										{item.likes}
									</span>
								</div>
							</div>
						</Card>
					</Link>
				))}
			</div>
		</div>
	);
}
