import { Button } from "@/components/ui/Button";
import { Avatar } from "@/components/ui/Avatar";
import type { BoardPostResponse } from "@/features/board/boardApi";
import { TYPE_TO_CATEGORY } from "@/features/qna/qnaConstants";
import { formatRelativeTime } from "@/lib/relativeTime";

interface QnaPostArticleProps {
	post: BoardPostResponse;
	canEdit: boolean;
	isAuthor: boolean;
	deleting: boolean;
	onEdit: () => void;
	onDelete: () => void;
}

export function QnaPostArticle({
	post,
	canEdit,
	isAuthor,
	deleting,
	onEdit,
	onDelete,
}: QnaPostArticleProps) {
	return (
		<article>
			<div className="mb-3 flex items-center justify-between gap-3">
				<div className="flex items-center gap-2">
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
					<span className="text-caption font-caption text-secondary">
						{TYPE_TO_CATEGORY[post.type]}
					</span>
				</div>
				{isAuthor && (
					<div className="flex gap-2">
						{canEdit && (
							<Button type="button" variant="secondary" size="sm" onClick={onEdit}>
								수정
							</Button>
						)}
						<Button
							type="button"
							variant="outline"
							size="sm"
							onClick={onDelete}
							disabled={deleting}
						>
							{deleting ? "삭제 중…" : "삭제"}
						</Button>
					</div>
				)}
			</div>
			<h1 className="mb-3 text-headline-lg font-display text-on-surface">
				{post.title}
			</h1>
			<div className="mb-5 flex items-center gap-2 text-caption font-caption text-secondary">
				<Avatar size={24} />
				{post.memberNickname} · {formatRelativeTime(post.createdAt)}
			</div>
			<div className="mb-5 whitespace-pre-wrap text-body-md text-on-surface">
				{post.content}
			</div>
			{post.images.length > 0 && (
				<div className="mb-5 grid grid-cols-2 gap-3">
					{[...post.images]
						.sort((a, b) => a.orderIndex - b.orderIndex)
						.map((image) => (
							<img
								key={image.id}
								src={image.url}
								alt={image.originalName ?? "첨부 이미지"}
								className="aspect-square rounded-lg object-cover"
							/>
						))}
				</div>
			)}
		</article>
	);
}
