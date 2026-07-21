import { cn } from "@/lib/cn";
import type { LocalChatMessage } from "@/features/chat/chatMessageModel";

interface ChatMessageBubbleProps {
	message: LocalChatMessage;
}

/**
 * 개별 메시지 말풍선.
 *
 * <ul>
 *   <li>내 메시지는 오른쪽 정렬 + primary 색상, 상대 메시지는 왼쪽 정렬 + 밝은 배경</li>
 *   <li>이미지 메시지는 상단에 썸네일, 하단에 (있으면) 본문</li>
 *   <li>내가 보낸 메시지에 readAt 이 있으면 하단에 "읽음" 표시</li>
 * </ul>
 */
export function ChatMessageBubble({ message }: ChatMessageBubbleProps) {
	const isMe = message.sender === "me";

	return (
		<div className={cn("flex flex-col", isMe ? "items-end" : "items-start")}>
			<div className={cn("flex", isMe ? "justify-end" : "justify-start")}>
				<div
					className={cn(
						"max-w-[70%] rounded-2xl px-4 py-2.5 text-body-md",
						isMe
							? "bg-primary text-on-primary"
							: "bg-surface-container-lowest text-on-surface",
					)}
				>
					{message.imageUrl && (
						<img
							src={message.imageUrl}
							alt={message.imageAlt ?? message.text ?? "첨부 이미지"}
							className="mb-2 max-h-64 rounded-lg"
						/>
					)}
					{message.text}
				</div>
			</div>
			{isMe && message.readAt && (
				<span className="mt-1 px-1 text-caption font-caption text-secondary">
					읽음
				</span>
			)}
		</div>
	);
}
