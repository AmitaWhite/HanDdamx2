import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Icon } from "@/components/ui/Icon";
import { formatRelativeTime } from "@/lib/relativeTime";

interface ChatConversationHeaderProps {
	displayName: string;
	avatarSrc: string;
	opponentMemberId: number;
	roomClosed: boolean;
	closedAt: string | null;
	closeBusy: boolean;
	onClose: () => void;
}

/**
 * 채팅 상세 상단 헤더. 뒤로가기, 상대 프로필 링크, ACTIVE 방의 종료 버튼.
 *
 * 종료·읽기 전용 여부는 부모(방 상태 관리 훅)에서 계산해 넘겨준다.
 */
export function ChatConversationHeader({
	displayName,
	avatarSrc,
	opponentMemberId,
	roomClosed,
	closedAt,
	closeBusy,
	onClose,
}: ChatConversationHeaderProps) {
	const profileHref = paths.creator(opponentMemberId);

	return (
		<div className="container-page flex items-center gap-3 border-b border-outline-variant/50 py-4">
			<Link to={paths.chat} aria-label="목록으로" className="text-on-surface">
				<Icon name="arrow_back" />
			</Link>
			<Link to={profileHref} className="flex min-w-0 flex-1 items-center gap-3">
				<Avatar src={avatarSrc} size={36} />
				<div className="min-w-0">
					<span className="block truncate text-label-md font-label-md text-on-surface">
						{displayName}
					</span>
					{roomClosed && (
						<span className="text-caption font-caption text-secondary">
							종료된 채팅방
							{closedAt ? ` · ${formatRelativeTime(closedAt)}` : ""}
						</span>
					)}
				</div>
			</Link>
			{!roomClosed && (
				<button
					type="button"
					onClick={onClose}
					disabled={closeBusy}
					className="shrink-0 rounded-full border border-outline-variant px-3 py-1.5 text-label-md font-label-md text-error transition-colors hover:bg-error-container/30 disabled:opacity-50"
				>
					{closeBusy ? "종료 중…" : "종료"}
				</button>
			)}
		</div>
	);
}
