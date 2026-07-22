import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { wsBaseUrl } from "@/lib/api";
import type { NotificationResponse } from "./notificationsApi";

/**
 * 알림 실시간 수신 STOMP 클라이언트.
 * 백엔드 ChatWebSocketConfig 기준: /ws(SockJS, ?access_token= 쿼리로 핸드셰이크 인증) 접속 후
 * 개인 토픽 /sub/notifications/{memberId} 구독(NotificationPublisher).
 */
export function connectNotificationSocket(
	memberId: number,
	accessToken: string,
	onNotification: (notification: NotificationResponse) => void,
): () => void {
	const client = new Client({
		webSocketFactory: () =>
			new SockJS(
				`${wsBaseUrl}?access_token=${encodeURIComponent(accessToken)}`,
			),
		reconnectDelay: 5000,
		onConnect: () => {
			client.subscribe(
				`/sub/notifications/${memberId}`,
				(message: IMessage) => {
					try {
						onNotification(JSON.parse(message.body) as NotificationResponse);
					} catch {
						// 파싱 실패한 프레임은 무시 — 실시간 알림 하나 놓치는 것뿐, 목록/배지는 REST로도 갱신됨
					}
				},
			);
		},
	});

	client.activate();

	return () => {
		void client.deactivate();
	};
}
