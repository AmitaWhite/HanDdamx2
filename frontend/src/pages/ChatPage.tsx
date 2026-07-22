import { useEffect, useState } from "react";
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { ChatComposer } from "@/components/chat/ChatComposer";
import { ChatConversationHeader } from "@/components/chat/ChatConversationHeader";
import { ChatMessageList } from "@/components/chat/ChatMessageList";
import { ChatRoomListItem } from "@/components/chat/ChatRoomListItem";
import { Alert } from "@/components/ui/Alert";
import { Card } from "@/components/ui/Card";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Icon } from "@/components/ui/Icon";
import { useAuth } from "@/features/auth/AuthContext";
import {
	type ChatRoomResponse,
	closeChatRoom,
	createOrGetChatRoom,
	getChatRoom,
	resolveChatOpponentId,
	sendChatImage,
} from "@/features/chat/chatApi";
import { useChatMessages } from "@/features/chat/useChatMessages";
import { useChatOpponent } from "@/features/chat/useChatOpponent";
import { useChatRoomList } from "@/features/chat/useChatRoomList";
import { useChatSocket } from "@/features/chat/useChatSocket";
import { ApiError } from "@/lib/api";
import { findCreator } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";

/** 쿼리스트링 → 양의 정수 id 파싱 헬퍼 */
function toNumericId(value: string | null): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isInteger(n) && n > 0 ? n : null;
}

/**
 * /chat 진입점. 쿼리스트링에 따라 세 가지 뷰로 분기한다.
 *
 * <ul>
 *   <li>{@code ?roomId=} — 대화 화면</li>
 *   <li>{@code ?creatorId=} — 크리에이터와의 방 조회/생성 후 roomId 로 리다이렉트</li>
 *   <li>(없음) — 참여 채팅방 목록</li>
 * </ul>
 */
export function ChatPage() {
	const [searchParams] = useSearchParams();
	const roomId = toNumericId(searchParams.get("roomId"));
	const creatorId = toNumericId(searchParams.get("creatorId"));

	if (roomId !== null) return <RemoteChatRoomPage roomId={roomId} />;
	if (creatorId !== null) {
		return <RemoteChatWithCreatorPage creatorId={creatorId} />;
	}
	return <ChatRoomListPage />;
}

/** 참여 채팅방 목록 (LDJ-018 / CHAT-005) */
function ChatRoomListPage() {
	const { user } = useAuth();
	const navigate = useNavigate();
	const isCreator = user?.role === "CREATOR";
	const { rooms, profiles, loading, error } = useChatRoomList(
		user?.memberId ?? null,
	);

	return (
		<div className="container-page py-6">
			<Card className="mx-auto max-w-2xl overflow-hidden">
				<div className="border-b border-outline-variant/50 p-5">
					<h1 className="text-headline-md font-display text-on-surface">
						메시지
					</h1>
					<p className="mt-1 text-body-md text-secondary">
						{isCreator
							? "나에게 메시지를 보낸 구독자 목록입니다."
							: "메시지를 주고받은 크리에이터 목록입니다."}
					</p>
				</div>

				{loading && (
					<p className="p-5 text-body-md text-secondary">불러오는 중…</p>
				)}

				{error && (
					<div className="p-5">
						<Alert variant="error">{error}</Alert>
					</div>
				)}

				{!loading && !error && rooms.length === 0 && (
					<p className="p-8 text-center text-body-md text-secondary">
						아직 대화한 상대가 없습니다.
					</p>
				)}

				<div className="divide-y divide-outline-variant/30">
					{rooms.map((room) => {
						const opponentId = user
							? resolveChatOpponentId(room, user.memberId)
							: 0;
						return (
							<ChatRoomListItem
								key={room.id}
								room={room}
								opponentId={opponentId}
								opponent={profiles.get(opponentId)}
								onClick={() => navigate(paths.chatRoom(room.id))}
							/>
						);
					})}
				</div>
			</Card>
		</div>
	);
}

/** ?creatorId= 진입 시 방을 만들거나 조회한 뒤 roomId URL 로 통일한다. */
function RemoteChatWithCreatorPage({ creatorId }: { creatorId: number }) {
	const navigate = useNavigate();
	const { isAuthenticated } = useAuth();
	const [loadError, setLoadError] = useState<string | null>(null);

	useEffect(() => {
		if (!isAuthenticated) return;

		let cancelled = false;
		createOrGetChatRoom(creatorId)
			.then((room) => {
				if (!cancelled) navigate(paths.chatRoom(room.id), { replace: true });
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setLoadError(
					err instanceof ApiError
						? err.message
						: "채팅방을 불러오지 못했습니다.",
				);
			});

		return () => {
			cancelled = true;
		};
	}, [creatorId, isAuthenticated, navigate]);

	if (!isAuthenticated) return <Navigate to={paths.login} replace />;

	if (loadError) {
		return (
			<div className="container-page py-6">
				<Link to={paths.chat} className="mb-4 inline-flex text-on-surface">
					<Icon name="arrow_back" />
				</Link>
				<Alert variant="error">{loadError}</Alert>
			</div>
		);
	}

	return (
		<p className="container-page py-6 text-body-md text-secondary">
			채팅방을 불러오는 중…
		</p>
	);
}

/** ?roomId= 진입 시 방 상세를 로드한 뒤 대화 화면(ChatConversation) 을 그린다. */
function RemoteChatRoomPage({ roomId }: { roomId: number }) {
	const { user, isAuthenticated } = useAuth();
	const [room, setRoom] = useState<ChatRoomResponse | null>(null);
	const [loading, setLoading] = useState(true);
	const [loadError, setLoadError] = useState<string | null>(null);

	useEffect(() => {
		if (!isAuthenticated || !user) return;

		let cancelled = false;
		setLoading(true);
		setLoadError(null);

		getChatRoom(roomId)
			.then((data) => {
				if (!cancelled) setRoom(data);
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setLoadError(
					err instanceof ApiError
						? err.message
						: "채팅방을 불러오지 못했습니다.",
				);
				setRoom(null);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, [roomId, isAuthenticated, user]);

	if (!isAuthenticated || !user) return <Navigate to={paths.login} replace />;

	if (loading) {
		return (
			<p className="container-page py-6 text-body-md text-secondary">
				채팅방을 불러오는 중…
			</p>
		);
	}

	if (loadError || !room) {
		return (
			<div className="container-page py-6">
				<Link to={paths.chat} className="mb-4 inline-flex text-on-surface">
					<Icon name="arrow_back" />
				</Link>
				<Alert variant="error">
					{loadError ?? "채팅방을 불러오지 못했습니다."}
				</Alert>
			</div>
		);
	}

	return (
		<ChatConversation
			room={room}
			onRoomChange={setRoom}
			opponentMemberId={resolveChatOpponentId(room, user.memberId)}
		/>
	);
}

interface ChatConversationProps {
	room: ChatRoomResponse;
	onRoomChange: (room: ChatRoomResponse) => void;
	opponentMemberId: number;
}

/**
 * 채팅 대화 화면 컨트롤러.
 *
 * 상태·API·WS 는 훅으로, 화면은 프레젠테이션 컴포넌트로 위임한다.
 *
 * <ul>
 *   <li>{@code useChatMessages} — REST 메시지 로딩·페이지네이션·읽음 처리</li>
 *   <li>{@code useChatSocket} — STOMP 텍스트 실시간 송수신</li>
 *   <li>{@code useChatOpponent} — 상대방 공개 프로필</li>
 *   <li>이미지 전송/방 종료는 REST — 여기서 직접 호출한 뒤 상태에 반영</li>
 * </ul>
 */
function ChatConversation({
	room,
	onRoomChange,
	opponentMemberId,
}: ChatConversationProps) {
	const { user, accessToken } = useAuth();
	const mockCreator = findCreator(String(opponentMemberId));
	const opponent = useChatOpponent(opponentMemberId);

	const {
		messages,
		loading: messagesLoading,
		error: messagesError,
		hasOlder,
		olderLoading,
		loadOlder,
		appendMessage,
		acknowledgeUnread,
	} = useChatMessages(room.id, user?.memberId ?? null);

	const [sendError, setSendError] = useState<string | null>(null);
	const [imageBusy, setImageBusy] = useState(false);
	const [closeBusy, setCloseBusy] = useState(false);
	const [closeConfirmOpen, setCloseConfirmOpen] = useState(false);

	const roomClosed = room.status === "CLOSED";

	const { connected: socketReady, sendText } = useChatSocket({
		chatRoomId: room.id,
		accessToken,
		enabled: !!user && !roomClosed,
		onMessage: (message) => {
			appendMessage(message);
			if (message.senderId !== user?.memberId) acknowledgeUnread();
		},
		onError: (error) => setSendError(error.message),
		onConnect: () => acknowledgeUnread(),
	});

	const canSendText = !roomClosed && socketReady && !imageBusy;
	const canSendImage = !roomClosed && !!user && !imageBusy;
	const displayName = opponent?.nickname ?? mockCreator.name;
	const avatarSrc =
		opponent?.profileImageUrl ?? mockImg(mockCreator.avatarSeed, 80, 80);

	function handleSendText(text: string) {
		setSendError(null);
		sendText(text);
	}

	async function handleSendImage(file: File) {
		if (!user || imageBusy) return;
		setSendError(null);
		setImageBusy(true);
		try {
			const saved = await sendChatImage(room.id, file);
			// 텍스트는 WS 수신으로만 반영 — 이미지는 REST 응답과 WS 브로드캐스트가 병렬로 오지만 id 중복 제거된다
			appendMessage(saved);
		} catch (err: unknown) {
			setSendError(
				err instanceof ApiError ? err.message : "이미지 전송에 실패했습니다.",
			);
		} finally {
			setImageBusy(false);
		}
	}

	function openCloseConfirm() {
		if (roomClosed || closeBusy) return;
		setCloseConfirmOpen(true);
	}

	async function handleCloseRoom() {
		if (roomClosed || closeBusy) return;
		setCloseConfirmOpen(false);
		setSendError(null);
		setCloseBusy(true);
		try {
			const closed = await closeChatRoom(room.id);
			onRoomChange(closed);
		} catch (err: unknown) {
			setSendError(
				err instanceof ApiError ? err.message : "채팅방 종료에 실패했습니다.",
			);
		} finally {
			setCloseBusy(false);
		}
	}

	return (
		<div className="flex h-[calc(100vh-72px)] flex-col">
			<ChatConversationHeader
				displayName={displayName}
				avatarSrc={avatarSrc}
				opponentMemberId={opponentMemberId}
				opponentIsCreator={opponent?.isCreator ?? false}
				roomClosed={roomClosed}
				closedAt={room.closedAt}
				closeBusy={closeBusy}
				onClose={openCloseConfirm}
			/>

			<ConfirmDialog
				open={closeConfirmOpen}
				title="채팅방을 종료할까요?"
				description="종료 후에는 메시지를 보낼 수 없고, 대화 내용만 조회할 수 있습니다."
				confirmLabel="종료"
				onConfirm={() => void handleCloseRoom()}
				onCancel={() => setCloseConfirmOpen(false)}
			/>

			{roomClosed && (
				<p className="container-page border-b border-outline-variant/30 py-3 text-body-md text-secondary">
					이 채팅방은 읽기 전용입니다. 메시지를 보낼 수 없습니다.
				</p>
			)}

			{sendError && (
				<div className="container-page py-2">
					<Alert variant="error">{sendError}</Alert>
				</div>
			)}

			{messagesError && (
				<div className="container-page py-2">
					<Alert variant="error">{messagesError}</Alert>
				</div>
			)}

			<ChatMessageList
				messages={messages}
				loading={messagesLoading}
				hasError={!!messagesError}
				hasOlder={hasOlder}
				olderLoading={olderLoading}
				onLoadOlder={() => void loadOlder()}
			/>

			<ChatComposer
				canSendText={canSendText}
				canSendImage={canSendImage}
				imageBusy={imageBusy}
				roomClosed={roomClosed}
				socketReady={socketReady}
				onSendText={handleSendText}
				onSelectImage={(file) => void handleSendImage(file)}
			/>
		</div>
	);
}
