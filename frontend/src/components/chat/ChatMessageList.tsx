import { useEffect, useRef } from "react";
import type { LocalChatMessage } from "@/features/chat/chatMessageModel";
import { ChatMessageBubble } from "./ChatMessageBubble";

interface ChatMessageListProps {
	messages: LocalChatMessage[];
	loading: boolean;
	hasError: boolean;
	hasOlder: boolean;
	olderLoading: boolean;
	onLoadOlder: () => void;
}

/**
 * 메시지 목록 스크롤 영역.
 *
 * 새 메시지가 추가될 때마다 하단으로 자동 스크롤한다.
 * "이전 메시지 보기" 는 상단에 두어 페이지네이션을 유도한다.
 */
export function ChatMessageList({
	messages,
	loading,
	hasError,
	hasOlder,
	olderLoading,
	onLoadOlder,
}: ChatMessageListProps) {
	const endRef = useRef<HTMLDivElement>(null);
	const lastMessageIdRef = useRef<string | null>(null);

	// 마지막 메시지 id 가 바뀔 때만 하단으로 스크롤한다.
	// - append(신규 수신·전송): 마지막 id 가 새로 생기므로 스크롤
	// - prepend(이전 페이지 로드): 마지막 id 는 그대로라 스크롤하지 않음
	useEffect(() => {
		const lastId = messages.length > 0 ? messages[messages.length - 1].id : null;
		if (lastId !== lastMessageIdRef.current) {
			lastMessageIdRef.current = lastId;
			endRef.current?.scrollIntoView({ behavior: "smooth" });
		}
	}, [messages]);

	return (
		<div className="container-page flex-1 space-y-3 overflow-y-auto py-6">
			{loading && (
				<p className="text-center text-body-md text-secondary">불러오는 중…</p>
			)}
			{!loading && hasOlder && (
				<div className="flex justify-center">
					<button
						type="button"
						onClick={onLoadOlder}
						disabled={olderLoading}
						className="text-body-md text-primary disabled:opacity-50"
					>
						{olderLoading ? "불러오는 중…" : "이전 메시지 보기"}
					</button>
				</div>
			)}
			{!loading && !hasError && messages.length === 0 && (
				<p className="text-center text-body-md text-secondary">
					아직 메시지가 없습니다.
				</p>
			)}
			{messages.map((message) => (
				<ChatMessageBubble key={message.id} message={message} />
			))}
			<div ref={endRef} />
		</div>
	);
}
