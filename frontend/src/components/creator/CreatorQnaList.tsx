import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import type { MockQnaPost } from "@/mocks/qna";

interface CreatorQnaListProps {
	qnaPosts: MockQnaPost[];
	emptyMessage?: string;
}

/** Q&A 게시글 리스트(필터 없음, 순수 리스트). QnaBoardPage와 CreatorPage의 인라인 미리보기가 공용으로 사용. */
export function CreatorQnaList({ qnaPosts, emptyMessage = "아직 Q&A가 없어요." }: CreatorQnaListProps) {
	return (
		<div className="divide-y divide-outline-variant/30 rounded-xl border border-outline-variant/50 bg-surface-container-lowest">
			{qnaPosts.map((post) => (
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
			{qnaPosts.length === 0 && <p className="p-8 text-center text-body-md text-secondary">{emptyMessage}</p>}
		</div>
	);
}
