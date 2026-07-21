import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api";
import {
	type ChatMessageResponse,
	getChatMessages,
	markChatMessagesAsRead,
	toChronologicalMessages,
} from "./chatApi";
import {
	type LocalChatMessage,
	appendUniqueMessage,
	toDisplayMessages,
} from "./chatMessageModel";

interface UseChatMessagesResult {
	messages: LocalChatMessage[];
	loading: boolean;
	error: string | null;
	hasOlder: boolean;
	olderLoading: boolean;
	loadOlder: () => Promise<void>;
	/** 새로 수신·전송된 서버 응답 1건을 목록에 병합 (id 중복 시 무시) */
	appendMessage: (message: ChatMessageResponse) => void;
	/** 300ms 디바운스 후 상대방 미확인 메시지 일괄 읽음 처리 */
	acknowledgeUnread: () => void;
}

const READ_ACK_DEBOUNCE_MS = 300;

/**
 * 채팅방의 메시지 상태·로딩·읽음 처리를 캡슐화한다.
 *
 * <ul>
 *   <li>방 진입 시 최근 50건을 desc 로 조회한 뒤 화면표시용 시간순으로 변환</li>
 *   <li>이전 페이지는 {@link loadOlder} 로 페치</li>
 *   <li>WS 수신은 {@link appendMessage} 로 병합 (id 중복 방지)</li>
 *   <li>{@link acknowledgeUnread} 는 진입·수신 시 호출 → 300ms 디바운스 후 read API</li>
 * </ul>
 */
export function useChatMessages(
	roomId: number,
	myMemberId: number | null,
): UseChatMessagesResult {
	const [messages, setMessages] = useState<LocalChatMessage[]>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
	const [hasOlder, setHasOlder] = useState(false);
	const [olderLoading, setOlderLoading] = useState(false);
	const [pageIndex, setPageIndex] = useState(0);

	const markReadTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

	const acknowledgeUnread = useCallback(() => {
		if (markReadTimerRef.current) clearTimeout(markReadTimerRef.current);
		markReadTimerRef.current = setTimeout(() => {
			void markChatMessagesAsRead(roomId).catch(() => {
				// 읽음 처리 실패는 채팅 표시·전송을 막지 않음
			});
		}, READ_ACK_DEBOUNCE_MS);
	}, [roomId]);

	useEffect(() => {
		if (myMemberId == null) return;

		let cancelled = false;
		setLoading(true);
		setError(null);
		setHasOlder(false);
		setPageIndex(0);

		getChatMessages(roomId)
			.then((page) => {
				if (cancelled) return;
				const chronological = toChronologicalMessages(page);
				setMessages(toDisplayMessages(chronological, myMemberId));
				setHasOlder(!page.last);
				setPageIndex(page.number);
				acknowledgeUnread();
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setMessages([]);
				setError(
					err instanceof ApiError
						? err.message
						: "메시지를 불러오지 못했습니다.",
				);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, [roomId, myMemberId, acknowledgeUnread]);

	useEffect(() => {
		return () => {
			if (markReadTimerRef.current) clearTimeout(markReadTimerRef.current);
		};
	}, []);

	const loadOlder = useCallback(async () => {
		if (myMemberId == null || olderLoading || !hasOlder) return;
		const nextPage = pageIndex + 1;
		setOlderLoading(true);
		setError(null);
		try {
			const page = await getChatMessages(roomId, nextPage);
			const older = toChronologicalMessages(page);
			setMessages((prev) => [
				...toDisplayMessages(older, myMemberId),
				...prev,
			]);
			setHasOlder(!page.last);
			setPageIndex(page.number);
		} catch (err: unknown) {
			setError(
				err instanceof ApiError
					? err.message
					: "이전 메시지를 불러오지 못했습니다.",
			);
		} finally {
			setOlderLoading(false);
		}
	}, [roomId, myMemberId, olderLoading, hasOlder, pageIndex]);

	const appendMessage = useCallback(
		(message: ChatMessageResponse) => {
			if (myMemberId == null) return;
			setMessages((prev) => appendUniqueMessage(prev, message, myMemberId));
		},
		[myMemberId],
	);

	return {
		messages,
		loading,
		error,
		hasOlder,
		olderLoading,
		loadOlder,
		appendMessage,
		acknowledgeUnread,
	};
}
