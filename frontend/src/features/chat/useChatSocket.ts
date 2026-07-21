import { useCallback, useEffect, useRef, useState } from "react";
import type { ChatMessageResponse } from "./chatApi";
import {
	type ChatSocketHandle,
	type ChatWebSocketErrorResponse,
	connectChatSocket,
} from "./chatSocket";

interface UseChatSocketOptions {
	chatRoomId: number;
	accessToken: string | null;
	/** false 면 연결하지 않음 (예: 종료된 방) */
	enabled: boolean;
	onMessage: (message: ChatMessageResponse) => void;
	onError?: (error: ChatWebSocketErrorResponse) => void;
	onConnect?: () => void;
}

interface UseChatSocketResult {
	/** STOMP CONNECT 완료 여부 */
	connected: boolean;
	/** 텍스트 메시지 publish. 미연결 시 no-op */
	sendText: (text: string) => void;
}

/**
 * 채팅방 STOMP WebSocket 연결을 React 라이프사이클에 맞춰 관리한다.
 *
 * 콜백은 ref 로 저장해 매 렌더마다 재연결되지 않는다.
 * roomId·accessToken·enabled 변경 시에만 재연결한다.
 */
export function useChatSocket({
	chatRoomId,
	accessToken,
	enabled,
	onMessage,
	onError,
	onConnect,
}: UseChatSocketOptions): UseChatSocketResult {
	const [connected, setConnected] = useState(false);
	const socketRef = useRef<ChatSocketHandle | null>(null);
	const handlersRef = useRef({ onMessage, onError, onConnect });

	useEffect(() => {
		handlersRef.current = { onMessage, onError, onConnect };
	});

	useEffect(() => {
		if (!enabled || !accessToken) {
			setConnected(false);
			return;
		}

		const socket = connectChatSocket({
			chatRoomId,
			accessToken,
			onMessage: (message) => handlersRef.current.onMessage(message),
			onError: (error) => handlersRef.current.onError?.(error),
			onConnect: () => {
				setConnected(true);
				handlersRef.current.onConnect?.();
			},
		});
		socketRef.current = socket;

		return () => {
			socket.disconnect();
			socketRef.current = null;
			setConnected(false);
		};
	}, [chatRoomId, accessToken, enabled]);

	const sendText = useCallback((text: string) => {
		socketRef.current?.sendText(text);
	}, []);

	return { connected, sendText };
}
