import type { ChatMessageResponse } from "./chatApi";

/**
 * 채팅 UI 렌더링용 메시지 모델.
 *
 * 서버 응답({@link ChatMessageResponse})을 시점·발신자·본문/이미지로 정규화한다.
 * `sender: "me" | "other"` 로 좌우 정렬만 결정하고, readAt 은 내가 보낸 메시지에만 남긴다.
 */
export interface LocalChatMessage {
	id: string;
	sender: "me" | "other";
	text?: string;
	imageUrl?: string;
	imageAlt?: string;
	/** 내가 보낸 메시지가 상대에게 읽힌 시각. 상대 메시지엔 undefined */
	readAt?: string | null;
}

/** 서버 응답 배열을 UI 모델 배열로 변환 */
export function toDisplayMessages(
	messages: ChatMessageResponse[],
	myMemberId: number,
): LocalChatMessage[] {
	return messages.map((m) => toDisplayMessage(m, myMemberId));
}

/** 단일 메시지 변환 */
export function toDisplayMessage(
	m: ChatMessageResponse,
	myMemberId: number,
): LocalChatMessage {
	return {
		id: String(m.id),
		sender: m.senderId === myMemberId ? "me" : "other",
		text: m.type === "TEXT" ? (m.content ?? "") : undefined,
		imageUrl: m.type === "IMAGE" ? (m.imageUrl ?? undefined) : undefined,
		imageAlt:
			m.type === "IMAGE" ? (m.imageOriginalName ?? "첨부 이미지") : undefined,
		readAt: m.senderId === myMemberId ? m.readAt : undefined,
	};
}

/**
 * 새 메시지를 목록에 추가한다.
 * 같은 id 가 이미 있으면(WebSocket 브로드캐스트와 REST 응답 중복 등) 그대로 반환.
 */
export function appendUniqueMessage(
	prev: LocalChatMessage[],
	message: ChatMessageResponse,
	myMemberId: number,
): LocalChatMessage[] {
	const id = String(message.id);
	if (prev.some((m) => m.id === id)) return prev;
	return [...prev, toDisplayMessage(message, myMemberId)];
}
