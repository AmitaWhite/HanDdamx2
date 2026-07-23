import { Avatar } from "@/components/ui/Avatar";
import {
	type ChatRoomListItemResponse,
	formatChatMessagePreview,
} from "@/features/chat/chatApi";
import type { OpponentProfile } from "@/features/chat/useChatOpponent";
import { cn } from "@/lib/cn";
import { formatRelativeTime } from "@/lib/relativeTime";

interface ChatRoomListItemProps {
	room: ChatRoomListItemResponse;
	opponentId: number;
	opponent: OpponentProfile | undefined;
	onClick: () => void;
}

/**
 * 채팅 목록의 한 줄. 아바타 + 닉네임 + 미리보기 + 시간 + 미읽음 배지.
 *
 * 표시용 fallback (닉네임 / 아바타)만 담당하고 목록 로딩·프로필 조회는 훅에서 처리한다.
 */
export function ChatRoomListItem({
	room,
	opponentId,
	opponent,
	onClick,
}: ChatRoomListItemProps) {
	const preview = formatChatMessagePreview(room.lastMessage);
	const timeLabel = room.lastMessage
		? formatRelativeTime(room.lastMessage.sentAt)
		: "";
	const isClosed = room.status === "CLOSED";
	const isUnread = room.unreadCount > 0;

	return (
		<button
			type="button"
			onClick={onClick}
			className="flex w-full items-center gap-3 p-5 text-left transition-colors hover:bg-surface-container-low"
		>
			<Avatar
				src={opponent?.profileImageUrl ?? undefined}
				fallbackText={opponent?.nickname ?? `회원 #${opponentId}`}
				size={48}
			/>
			<div className="min-w-0 flex-1">
				<div className="flex items-center justify-between gap-2">
					<p
						className={cn(
							"truncate text-label-md font-label-md text-on-surface",
							isUnread && "font-bold",
						)}
					>
						{opponent?.nickname ?? `회원 #${opponentId}`}
					</p>
					{timeLabel && (
						<span className="shrink-0 text-caption font-caption text-secondary">
							{timeLabel}
						</span>
					)}
				</div>
				<div className="mt-1 flex items-center justify-between gap-2">
					<p
						className={cn(
							"truncate text-body-md",
							isUnread ? "font-medium text-on-surface" : "text-secondary",
						)}
					>
						{preview}
						{isClosed && " · 종료됨"}
					</p>
					{isUnread && (
						<span className="flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-primary px-1.5 text-caption font-caption text-on-primary">
							{room.unreadCount > 99 ? "99+" : room.unreadCount}
						</span>
					)}
				</div>
			</div>
		</button>
	);
}
