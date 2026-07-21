import { ensureFreshAccessToken, http, unwrap } from "@/lib/api";

/** 백엔드 ChatRoomStatus (com.white.handdam.chat.entity.ChatRoomStatus) */
export type ChatRoomStatus = "ACTIVE" | "CLOSED";

/** 백엔드 ChatMessageType */
export type ChatMessageType = "TEXT" | "IMAGE";

/** 백엔드 ChatMessagePreviewResponse */
export interface ChatMessagePreviewResponse {
	id: number;
	senderId: number;
	type: ChatMessageType;
	content: string | null;
	imageUrl: string | null;
	sentAt: string;
	readAt: string | null;
}

/** Spring Data Page (메시지 내역) */
export interface ChatMessagePageResponse {
	content: ChatMessageResponse[];
	number: number;
	size: number;
	totalElements: number;
	totalPages: number;
	first: boolean;
	last: boolean;
	empty: boolean;
}

/** 백엔드 ChatMessageResponse */
export interface ChatMessageResponse {
	id: number;
	chatRoomId: number;
	senderId: number;
	type: ChatMessageType;
	content: string | null;
	imageUrl: string | null;
	imageOriginalName: string | null;
	readAt: string | null;
	sentAt: string;
}

/** 백엔드 ChatRoomResponse */
export interface ChatRoomResponse {
	id: number;
	creatorId: number;
	memberId: number;
	status: ChatRoomStatus;
	closedBy: number | null;
	closedAt: string | null;
	lastMessageAt: string | null;
	createdAt: string;
	updatedAt: string;
}

/** 백엔드 ChatRoomListItemResponse */
export interface ChatRoomListItemResponse {
	id: number;
	creatorId: number;
	memberId: number;
	status: ChatRoomStatus;
	closedBy: number | null;
	closedAt: string | null;
	lastMessageAt: string | null;
	createdAt: string;
	updatedAt: string;
	lastMessage: ChatMessagePreviewResponse | null;
	unreadCount: number;
}

/**
 * 참여 채팅방 목록 + 마지막 메시지·미읽음 수 (LDJ-018 / CHAT-005).
 * 백엔드: GET /api/chat-rooms
 * 권한: 로그인 사용자(본인이 creator 또는 member인 방만).
 */
export function getMyChatRooms() {
	return unwrap<ChatRoomListItemResponse[]>(http.get("/chat-rooms"));
}

/**
 * 채팅방 상세·상태 조회 (LDJ-019 / CHAT-009).
 * 백엔드: GET /api/chat-rooms/{chatRoomId}
 */
export function getChatRoom(chatRoomId: number) {
	return unwrap<ChatRoomResponse>(http.get(`/chat-rooms/${chatRoomId}`));
}

/**
 * 채팅 메시지 내역 조회 (LDJ-020 / CHAT-006).
 * 백엔드: GET /api/chat-rooms/{chatRoomId}/messages
 * 기본 정렬 sentAt,desc — UI에서는 reverse 해서 시간순 표시.
 */
export function getChatMessages(
	chatRoomId: number,
	page = 0,
	size = 50,
	sort: "sentAt,asc" | "sentAt,desc" = "sentAt,desc",
) {
	return unwrap<ChatMessagePageResponse>(
		http.get(`/chat-rooms/${chatRoomId}/messages`, {
			params: { page, size, sort },
		}),
	);
}

/** desc 페이지를 채팅 UI용 시간순(오래된 것 먼저)으로 변환 */
export function toChronologicalMessages(
	page: ChatMessagePageResponse,
): ChatMessageResponse[] {
	return [...page.content].reverse();
}

/** 백엔드 ChatReadResponse */
export interface ChatReadResponse {
	updatedCount: number;
}

/**
 * 상대방 미확인 메시지 일괄 읽음 (LDJ-022 / CHAT-007).
 * 백엔드: PATCH /api/chat-rooms/{chatRoomId}/read
 */
export function markChatMessagesAsRead(chatRoomId: number) {
	return unwrap<ChatReadResponse>(http.patch(`/chat-rooms/${chatRoomId}/read`));
}

/**
 * 이미지 메시지 전송 (LDJ-021 / CHAT-004).
 * 텍스트는 WebSocket — 이미지만 REST multipart.
 * 백엔드: POST /api/chat-rooms/{chatRoomId}/messages
 * 저장 후 `/sub/chat-rooms/{id}` 로 브로드캐스트된다.
 */
export async function sendChatImage(chatRoomId: number, image: File) {
	await ensureFreshAccessToken();
	const formData = new FormData();
	formData.append("type", "IMAGE");
	formData.append("image", image, image.name);
	return unwrap<ChatMessageResponse>(
		http.post(`/chat-rooms/${chatRoomId}/messages`, formData),
	);
}

/**
 * 채팅방 종료·읽기 전용 전환 (LDJ-023 / CHAT-008~010).
 * 백엔드: PATCH /api/chat-rooms/{chatRoomId}/close
 * 권한: 채팅방 참여자(creator 또는 member).
 */
export function closeChatRoom(chatRoomId: number) {
	return unwrap<ChatRoomResponse>(
		http.patch(`/chat-rooms/${chatRoomId}/close`),
	);
}

/**
 * 백엔드: POST /api/creators/{creatorId}/chat-rooms
 * 권한: 활성 유료 구독자 (JWT). 신규 201 / 기존 200.
 */
export function createOrGetChatRoom(creatorId: number) {
	return unwrap<ChatRoomResponse>(
		http.post(`/creators/${creatorId}/chat-rooms`),
	);
}

/** 목록·상세에서 상대방 memberId */
export function resolveChatOpponentId(
	room: Pick<ChatRoomResponse, "creatorId" | "memberId">,
	myMemberId: number,
): number {
	return room.creatorId === myMemberId ? room.memberId : room.creatorId;
}

export function formatChatMessagePreview(
	message: ChatMessagePreviewResponse | null,
): string {
	if (!message) return "";
	if (message.type === "IMAGE") return "[이미지]";
	return message.content?.trim() || "";
}

/** 목록 노출: 실제 메시지가 한 건이라도 있는 방만 */
export function isChatRoomWithMessages(
	room: Pick<ChatRoomListItemResponse, "lastMessage">,
): boolean {
	return room.lastMessage !== null;
}
