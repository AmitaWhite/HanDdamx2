import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import {
	type BoardPostResponse,
	type BoardPostType,
	getPremiumBoardPosts,
} from "@/features/board/boardApi";
import { ApiError } from "@/lib/api";
import { findCreator } from "@/mocks/creators";
import { mockQnaPosts, type QnaCategory } from "@/mocks/qna";

const CATEGORIES: readonly (QnaCategory | "전체")[] = [
	"전체",
	"제작 질문",
	"작품 피드백",
	"콘텐츠 제안",
	"재료 추천",
	"일반 소통",
] as const;

/** 카테고리 라벨 → 백엔드 BoardPostType 매핑. "전체"는 서버 필터 미포함. */
const CATEGORY_TO_TYPE: Record<QnaCategory, BoardPostType> = {
	"제작 질문": "QUESTION",
	"작품 피드백": "FEEDBACK",
	"콘텐츠 제안": "CONTENT_SUGGESTION",
	"재료 추천": "MATERIAL",
	"일반 소통": "GENERAL",
};

/** 백엔드 BoardPostType → 카테고리 라벨 (목록 표시용). */
const TYPE_TO_CATEGORY: Record<BoardPostType, QnaCategory> = {
	QUESTION: "제작 질문",
	FEEDBACK: "작품 피드백",
	CONTENT_SUGGESTION: "콘텐츠 제안",
	MATERIAL: "재료 추천",
	GENERAL: "일반 소통",
};

const PAGE_SIZE = 20;

/** URL 파라미터의 creatorId가 실제 숫자 ID이면 API를, 아니면 mock을 쓴다. */
function toNumericCreatorId(value: string): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isInteger(n) && n > 0 ? n : null;
}

/** ISO 문자열 → "n분/시간/일 전" 간단 라벨. */
function toRelativeLabel(iso: string): string {
	const diffMs = Date.now() - new Date(iso).getTime();
	if (Number.isNaN(diffMs) || diffMs < 0) return "방금";
	const minutes = Math.floor(diffMs / 60_000);
	if (minutes < 1) return "방금";
	if (minutes < 60) return `${minutes}분 전`;
	const hours = Math.floor(minutes / 60);
	if (hours < 24) return `${hours}시간 전`;
	const days = Math.floor(hours / 24);
	if (days < 7) return `${days}일 전`;
	const weeks = Math.floor(days / 7);
	return `${weeks}주 전`;
}

export function QnaBoardPage() {
	const { creatorId = "" } = useParams();
	const creator = findCreator(creatorId);
	const numericCreatorId = toNumericCreatorId(creatorId);
	const useRemote = numericCreatorId !== null;

	const [active, setActive] = useState<(typeof CATEGORIES)[number]>("전체");
	const [remotePosts, setRemotePosts] = useState<BoardPostResponse[]>([]);
	const [loading, setLoading] = useState(false);
	const [errorMessage, setErrorMessage] = useState<string | null>(null);

	// 원격 API 호출: URL creatorId가 숫자일 때만.
	useEffect(() => {
		if (!useRemote || numericCreatorId === null) return;

		let cancelled = false;
		setLoading(true);
		setErrorMessage(null);

		getPremiumBoardPosts({
			creatorId: numericCreatorId,
			type: active === "전체" ? undefined : CATEGORY_TO_TYPE[active],
			page: 0,
			size: PAGE_SIZE,
		})
			.then((page) => {
				if (!cancelled) setRemotePosts(page.content);
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				if (err instanceof ApiError) {
					setErrorMessage(err.message);
				} else {
					setErrorMessage("게시글을 불러오지 못했습니다.");
				}
				setRemotePosts([]);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, [useRemote, numericCreatorId, active]);

	// 화면용 통합 목록: 원격 모드면 API 결과, 아니면 mock 필터.
	const items = useRemote
		? remotePosts.map((post) => ({
				id: String(post.id),
				title: post.title,
				status: post.status === "ANSWERED" ? "answered" : "pending",
				category: TYPE_TO_CATEGORY[post.type],
				authorName: post.memberNickname,
				commentCount: 0, // 백엔드 확장 예정
				createdAtLabel: toRelativeLabel(post.createdAt),
			}))
		: mockQnaPosts
				.filter((q) => q.creatorId === creator.id)
				.filter((q) => active === "전체" || q.category === active)
				.map((q) => ({
					id: q.id,
					title: q.title,
					status: q.status,
					category: q.category,
					authorName: q.authorName,
					commentCount: q.commentCount,
					createdAtLabel: q.createdAtLabel,
				}));

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
				{numericCreatorId !== null ? (
					<LinkButton to={paths.creatorQnaNew(numericCreatorId)}>글쓰기</LinkButton>
				) : (
					<Button type="button" disabled title="숫자 creatorId 에서만 작성 가능">
						글쓰기
					</Button>
				)}
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

			{errorMessage && (
				<div className="mb-4 rounded-xl border border-primary/30 bg-primary/5 p-4 text-body-md text-primary">
					{errorMessage}
				</div>
			)}

			<div className="divide-y divide-outline-variant/30 rounded-xl border border-outline-variant/50 bg-surface-container-lowest">
				{loading && (
					<p className="p-8 text-center text-body-md text-secondary">불러오는 중…</p>
				)}
				{!loading &&
					items.map((post) => (
						<Link
							key={post.id}
							to={paths.qnaPost(post.id)}
							className="flex items-center gap-4 p-5 transition-colors hover:bg-surface-container-low"
						>
							<span
								className={
									"shrink-0 rounded px-2 py-1 text-[10px] font-bold " +
									(post.status === "answered" ? "bg-primary text-on-primary" : "bg-surface-container text-secondary")
								}
							>
								{post.status === "answered" ? "답변 완료" : "답변 대기"}
							</span>
							<div className="min-w-0 flex-1">
								<p className="truncate text-body-md text-on-surface">{post.title}</p>
								<p className="mt-1 text-caption font-caption text-secondary">
									{post.category} · {post.authorName} · 댓글 {post.commentCount} · {post.createdAtLabel}
								</p>
							</div>
						</Link>
					))}
				{!loading && items.length === 0 && !errorMessage && (
					<p className="p-8 text-center text-body-md text-secondary">해당 카테고리의 글이 아직 없어요.</p>
				)}
			</div>
			<div className="mt-6 text-center">
				<Button variant="secondary">더보기</Button>
			</div>
		</div>
	);
}
