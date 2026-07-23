import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { backendOrigin } from "@/lib/api";
import type { ChatMessageResponse } from "./chatApi";

/** 백엔드 ChatWebSocketErrorResponse */
export interface ChatWebSocketErrorResponse {
	code: string;
	message: string;
}

export interface ChatSocketHandle {
	sendText: (content: string) => void;
	disconnect: () => void;
}

/**
 * 채팅방 STOMP WebSocket.
 *
 * - 연결: GET SockJS `${backendOrigin}/ws?access_token=...`
 * - 구독: `/sub/chat-rooms/{chatRoomId}` (수신)
 * - 송신: `/pub/chat-rooms/{chatRoomId}/messages` (TEXT)
 * - 에러: `/user/queue/errors`
 */
export function connectChatSocket(options: {
	chatRoomId: number;
	accessToken: string;
	onMessage: (message: ChatMessageResponse) => void;
	onError?: (error: ChatWebSocketErrorResponse) => void;
	onConnect?: () => void;
}): ChatSocketHandle {
	const { chatRoomId, accessToken, onMessage, onError, onConnect } = options;

	const client = new Client({
		webSocketFactory: () =>
			new SockJS(
				`${backendOrigin}/ws?access_token=${encodeURIComponent(accessToken)}`,
			),
		reconnectDelay: 5000,
		connectHeaders: {
			Authorization: `Bearer ${accessToken}`,
		},
		onConnect: () => {
			client.subscribe(`/sub/chat-rooms/${chatRoomId}`, (message: IMessage) => {
				try {
					onMessage(JSON.parse(message.body) as ChatMessageResponse);
				} catch {
					// 파싱 실패 프레임 무시
				}
			});
			client.subscribe("/user/queue/errors", (message: IMessage) => {
				try {
					onError?.(JSON.parse(message.body) as ChatWebSocketErrorResponse);
				} catch {
					// ignore
				}
			});
			onConnect?.();
		},
	});

	client.activate();

	return {
		sendText(content: string) {
			if (!client.connected) return;
			client.publish({
				destination: `/pub/chat-rooms/${chatRoomId}/messages`,
				body: JSON.stringify({ type: "TEXT", content }),
			});
		},
		disconnect() {
			void client.deactivate();
		},
	};
}
