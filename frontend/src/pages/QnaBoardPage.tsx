import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { CreatorQnaList } from "@/components/creator/CreatorQnaList";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { findCreator } from "@/mocks/creators";
import { mockQnaPosts, type QnaCategory } from "@/mocks/qna";

const CATEGORIES: (QnaCategory | "전체")[] = ["전체", "제작 질문", "작품 피드백", "콘텐츠 제안", "재료 추천", "일반 소통"];

export function QnaBoardPage() {
	const { creatorId = "" } = useParams();
	const creator = findCreator(creatorId);
	const [active, setActive] = useState<(typeof CATEGORIES)[number]>("전체");

	const posts = mockQnaPosts.filter((q) => q.creatorId === creator.id && (active === "전체" || q.category === active));

	return (
		<div className="container-page py-6">
			<Link
				to={paths.creator(creator.id)}
				className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
			>
				<Icon name="arrow_back" className="text-[18px]" />
				작가 페이지로
			</Link>

			<div className="mb-6 flex items-center justify-between">
				<div>
					<h1 className="text-headline-md font-display text-on-surface">{creator.name} 작가 · Q&A 게시판</h1>
					<p className="mt-1 text-caption font-caption text-secondary">유료 구독자 전용 공간입니다</p>
				</div>
				<Button>글쓰기</Button>
			</div>

			<div className="-mx-margin-mobile mb-6 overflow-x-auto px-margin-mobile md:mx-0 md:px-0">
				<div className="flex gap-2 whitespace-nowrap">
					{CATEGORIES.map((c) => (
						<button key={c} type="button" onClick={() => setActive(c)}>
							<Chip active={active === c}>{c}</Chip>
						</button>
					))}
				</div>
			</div>

			<CreatorQnaList qnaPosts={posts} emptyMessage="해당 카테고리의 글이 아직 없어요." />

			<div className="mt-6 text-center">
				<Button variant="secondary">더보기</Button>
			</div>
		</div>
	);
}
