import { Icon } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

interface EngagementBarProps {
	likeCount: number;
	commentCount: number;
	liked: boolean;
	onToggleLike: () => void;
	className?: string;
}

/** 좋아요/댓글 액션 바. post-detail·qna-post에서 공용. */
export function EngagementBar({ likeCount, commentCount, liked, onToggleLike, className }: EngagementBarProps) {
	return (
		<div className={cn("flex items-center gap-5", className)}>
			<button
				type="button"
				onClick={onToggleLike}
				aria-label={liked ? "좋아요 취소" : "좋아요"}
				aria-pressed={liked}
				className={cn(
					"flex items-center gap-1.5 text-label-md font-label-md transition-colors",
					liked ? "text-primary" : "text-secondary hover:text-on-surface",
				)}
			>
				<Icon name="favorite" filled={liked} className="text-[20px]" />
				{likeCount}
			</button>
			<span className="flex items-center gap-1.5 text-label-md font-label-md text-secondary">
				<Icon name="chat_bubble_outline" className="text-[20px]" />
				{commentCount}
			</span>
		</div>
	);
}
